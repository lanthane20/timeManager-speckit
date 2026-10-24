# Guide de démarrage développeur — TimePlanning

**Généré le** : 2026-06-02 · **Vérifié le** : 2026-09-06 (T145, déroulé étape par étape)

---

## Prérequis

| Outil | Version minimale | Rôle |
|-------|-----------------|------|
| Java (JDK) | 25 | Runtime et compilation backend |
| Maven | (fourni) | Le wrapper `./mvnw` est versionné — aucune installation requise |
| Node.js | 22 LTS | Build frontend Angular |
| Docker Desktop | 4.x | Base de données de développement (optionnel si mariadb4j) |
| Git | 2.40+ | Gestion du code source |

> **Note** : Le backend embarque `mariadb4j` pour le profil `dev` — aucun Docker requis pour démarrer.

---

## Clonage et structure

```bash
git clone <repo-url>
cd timeManager
```

Structure des modules Maven :
```
timeManager/
├── pom.xml          # Parent BOM
├── domain/          # Domaine métier pur Java
├── application/     # Cas d'usage et ports
├── infrastructure/  # Adaptateurs JPA, email, Excel
├── api/             # Contrôleurs Spring MVC
└── frontend/        # SPA Angular + PrimeNG
```

---

## Configuration locale

### Backend

Copier le fichier d'exemple :

```bash
cp .env.example .env
```

Variables obligatoires dans `.env` :

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=<votre-client-id-google>
GOOGLE_CLIENT_SECRET=<votre-client-secret-google>

# JWT (issuer Google)
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://accounts.google.com

# Base de données (ignoré en profil dev — mariadb4j gère tout)
DB_URL=jdbc:mariadb://localhost:3306/timemanager
DB_USERNAME=timemanager
DB_PASSWORD=<mot-de-passe>

# Email (désactivable en dev via NOTIFICATION_ENABLED=false)
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=<user>
MAIL_PASSWORD=<password>
NOTIFICATION_ENABLED=false
```

### Frontend

`src/environments/environment.ts` est versionné avec des valeurs de développement. Y remplacer le
`client_id` par votre Client ID Google OAuth2 :

```bash
cd frontend
# Éditer src/environments/environment.ts : champ oidc.client_id
```

`environment.example.ts` sert de référence pour les variables attendues.

Le build de production utilise `environment.prod.ts`, dont les marqueurs sont remplacés dans le
build par les variables du déploiement (`GOOGLE_CLIENT_ID`, `APP_BASE_URL` de `.env.example`) :

```bash
cd frontend
npm run build -- --configuration=production
GOOGLE_CLIENT_ID=<client-id> APP_BASE_URL=https://timemanager.internal npm run inject-env
```

Le script échoue si une variable manque ou contient un caractère hors `[A-Za-z0-9_.:/-]`, et si le
build ne contient aucun marqueur (déjà injecté, ou build sans `environment.prod.ts`).

Le déploiement sur serveur privé (images Docker, Traefik, runner auto-hébergé) est décrit dans
[`docker/production/README.md`](../../docker/production/README.md) ; l'image frontend réalise
cette injection à son démarrage.

---

## Démarrage en développement

### Backend (profil `dev` — mariadb4j embarqué)

```bash
cd timeManager
./mvnw spring-boot:run -pl api -Dspring-boot.run.profiles=dev
```

L'API démarre sur `http://localhost:8080`. Liquibase applique automatiquement les migrations au démarrage.

### Frontend

```bash
cd frontend
npm install --legacy-peer-deps
npm start
```

> **`--legacy-peer-deps` est nécessaire** pour les paquets `@angular-eslint` 18 (outillage de
> développement) ; PrimeNG est aligné sur Angular 21. Voir
> [`security-review.md`](./security-review.md) (A06).

L'application Angular démarre sur `http://localhost:4200` avec proxy vers `http://localhost:8080/api`.

---

## Lancer les tests

### Tests unitaires (domaine + application — sans Spring)

```bash
./mvnw test -pl domain,application
```

### Tests d'intégration (JPA + Liquibase — Testcontainers)

```bash
./mvnw verify -pl infrastructure
```

> Testcontainers démarre automatiquement un conteneur MariaDB pour les tests d'intégration. Docker doit être disponible.

### Tests des contrôleurs (MockMvc)

```bash
./mvnw test -pl api -am
```

> Le `-am` est nécessaire : sans lui, Maven utilise les artefacts `domain`, `application` et
> `infrastructure` installés dans le dépôt local, qui peuvent être obsolètes.

### Tous les tests + rapport Allure

```bash
./mvnw verify
./mvnw allure:report
# Rapport : target/site/allure-maven-plugin/index.html
```

> `verify` inclut les tests d'intégration Testcontainers : **Docker doit être démarré**. Sans Docker,
> utiliser `./mvnw test -DskipITs`.

### Tests frontend

```bash
cd frontend
npm test              # Vitest (unitaires)
npm run e2e:install   # une seule fois : télécharge les navigateurs Playwright
npm run e2e           # Playwright (end-to-end)
```

Les scénarios end-to-end s'exécutent contre l'application complète (MariaDB, backend, frontend)
et se connectent réellement : un Keycloak de test remplace Google, avec trois comptes de
démonstration (`frontend/e2e/keycloak/timeplanning-realm.json`) dont les rôles sont en base
(`frontend/e2e/fixtures/seed.sql`). Sans la variable `E2E_KEYCLOAK_URL`, ils sont ignorés. La CI
(job `e2e`) déroule les mêmes étapes :

```bash
# 1. Keycloak de test (http://localhost:8180, royaume « timeplanning »)
docker compose -f docker/docker-compose.yml --profile e2e up -d keycloak

# 2. Backend sur une base MariaDB dédiée et vide, émetteur des jetons = Keycloak
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8180/realms/timeplanning \
NOTIFICATION_ENABLED=false MANAGEMENT_HEALTH_MAIL_ENABLED=false \
java -jar api/target/api-1.0.0-SNAPSHOT.jar

# 3. Comptes de démonstration (après le démarrage, qui applique les migrations)
mariadb -utimemanager -p timemanager < frontend/e2e/fixtures/seed.sql

# 4. Tests (hors CI, Playwright démarre lui-même `npm run start:e2e`)
cd frontend && E2E_KEYCLOAK_URL=http://localhost:8180 npm run e2e
```

La configuration Angular `e2e` (`npm run start:e2e`) ne diffère de `development` que par
l'émetteur OIDC (`src/environments/environment.e2e.ts`).

> Les projets bureau et mobile s'exécutent sur la même base, chacun sur son propre mois : repartir
> d'une base vide avant chaque exécution complète.

---

## Conventions de développement

### Nommage et formatage

- **Java** : Google Java Format via Spotless — `./mvnw spotless:check` en CI, `./mvnw spotless:apply`
  pour corriger. Noms explicites, pas d'abréviations.
- **TypeScript** : ESLint (flat config) + Prettier — `npm run lint` / `npm run format`.
- **Complexité** : Checkstyle bloque si cyclomatique > 10. La configuration partagée est
  `config/checkstyle/checkstyle.xml` et la vérification s'exécute à la phase `validate` de chaque
  module — donc à chaque build, sans commande dédiée.

### Workflow Git

```bash
# Toujours travailler sur la branche de fonctionnalité
git checkout 001-timeplanning-gestion-absences

# Commits atomiques et descriptifs
git commit -m "feat(absence): implement working days calculator (EF-002)"
```

Pas de push direct sur `master` — toujours via PR avec revue.

### Cycle TDD

1. Écrire le test JGiven (scénario Given/When/Then depuis la spec).
2. Vérifier qu'il échoue (`mvn test` → RED).
3. Implémenter le minimum pour le faire passer (GREEN).
4. Refactorer sans casser les tests (REFACTOR).

---

## Sécurité locale

Pour tester les endpoints protégés en local, obtenir un JWT Google :

```bash
# Via curl avec votre access token Google (obtenu depuis l'interface OIDC du frontend)
curl -H "Authorization: Bearer <votre-jwt>" http://localhost:8080/api/v1/absences
```

Chaque requête doit porter un header `Authorization: Bearer <jwt>`.

---

## Migrations Liquibase

Les migrations sont dans `infrastructure/src/main/resources/db/changelog/`.

Elles sont appliquées automatiquement au démarrage de l'API (`spring.liquibase.enabled=true`) et
vérifiées en isolation par `LiquibaseMigrationIT`.

Le `liquibase-maven-plugin` n'est pas configuré : les goals `liquibase:update` / `liquibase:rollback`
ne sont donc pas disponibles en ligne de commande. Pour rejouer les migrations, redémarrer l'API sur
une base vierge.

Chaque nouveau changeset doit inclure un bloc `<rollback>` explicite.

---

## Logs structurés

Les logs en développement sont au format texte (lisible). En production (profil `prod`), ils basculent en JSON via `logstash-logback-encoder`.

Filtrer les logs par `correlation_id` en prod :

```bash
jq 'select(.correlation_id == "uuid-de-la-requête")' app.log
```

---

## Contacts et ressources

- **Spec fonctionnelle** : [`specs/001-timeplanning-gestion-absences/spec.md`](./spec.md)
- **Modèle de données** : [`data-model.md`](./data-model.md)
- **Décisions techniques** : [`research.md`](./research.md)
- **Contrats API** : [`contracts/openapi.yaml`](./contracts/openapi.yaml) — la documentation vivante
  est exposée par l'application sur `/swagger-ui.html` et `/v3/api-docs`
- **Revue de sécurité** : [`security-review.md`](./security-review.md)
- **Revue d'accessibilité** : [`accessibility-review.md`](./accessibility-review.md)
- **Constitution du projet** : [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
