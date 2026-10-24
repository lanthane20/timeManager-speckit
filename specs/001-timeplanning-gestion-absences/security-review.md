# Revue de sécurité OWASP Top 10 — TimePlanning

**Tâche** : T144 · **Périmètre** : tous les endpoints `/api/v1/**` livrés en Phases 3 à 9
**Référence** : Constitution Principe II (Sécurité par défaut) et Principe IV (Séparation des permissions)

---

## Méthode

Chaque endpoint a été relu au regard des dix catégories OWASP Top 10 (2021). Pour chaque risque, la
mesure en place est indiquée ainsi que le test qui la couvre. Les points sans mesure automatisée sont
listés en fin de document comme actions restantes.

---

## Inventaire des endpoints et garde d'autorisation

| Endpoint | Méthode | Rôle exigé | Contrôle de périmètre supplémentaire |
|----------|---------|------------|--------------------------------------|
| `/api/v1/absences` | POST | SALARIE | Absence créée pour l'utilisateur du jeton uniquement |
| `/api/v1/absences/{id}` | DELETE | SALARIE | `CancelAbsenceRequest` refuse une absence d'autrui |
| `/api/v1/absences/{id}/modify` | PUT | SALARIE | `RequestAbsenceModification` refuse une absence d'autrui |
| `/api/v1/absences/me`, `/me/counters` | GET | SALARIE | Identité issue du jeton, jamais d'un paramètre |
| `/api/v1/absence-types` | GET | SALARIE | Référentiel en lecture seule, sans règle de gestion interne |
| `/api/v1/manager/absences/pending`, `/pending-n2` | GET | MANAGER | Filtré sur les équipes encadrées |
| `/api/v1/manager/absences/{id}/validate`, `/validate-n2`, `/refuse`, `/cancel` | PUT/DELETE | MANAGER | `ManagerPerimeter` + interdiction d'auto-validation |
| `/api/v1/manager/employees/{employeeId}/absences`, `/counters` | POST/GET | MANAGER | `ManagerPerimeter.assertManagesEmployee` |
| `/api/v1/planning`, `/planning/teams` | GET | MANAGER ou RH | Équipes managées et leurs sous-équipes ; toutes les équipes pour la RH |
| `/api/v1/me` | GET | authentifié | Profil et rôles de l'utilisateur du jeton uniquement |
| `/api/v1/rh/**` | toutes | RH | Rôle RH exigé dans le use case **et** au niveau HTTP |
| `/api/v1/recurrences/**` | toutes | SALARIE | `recurrenceEnabled` vérifié dans le use case (EF-030) |
| `/api/v1/admin/**` | toutes | ADMIN | Aucun endpoint n'expose de donnée d'absence (EF-035) |

---

## Analyse par risque

### A01 — Broken Access Control

**Risque principal du produit** (4 rôles, données personnelles d'absence).

- Toute la logique d'autorisation est centralisée dans `AuthorizationService` et `ManagerPerimeter`
  (couche `application`), jamais en ligne dans un contrôleur — Principe IV.
- Double barrière : `@PreAuthorize` au niveau HTTP **et** vérification de rôle dans le use case. Un
  contrôleur mal annoté ne suffit donc pas à ouvrir une donnée.
- **IDOR** : aucun endpoint « mes données » n'accepte d'identifiant en paramètre — l'identité vient
  systématiquement du jeton (`CurrentUserProvider.resolve`). Les endpoints portant un `employeeId`
  (`/manager/employees/**`, `/rh/employees/**`) passent par un contrôle de périmètre explicite.
- Auto-validation interdite (EF-010) : `assertCannotSelfApprove` est appelée avant toute décision, et
  la file de validation exclut les demandes du manager lui-même.
- Tout refus (403) émet un événement d'audit `ACCES_REFUSE` via `AccessDeniedAuditHandler`.

**Couverture de test** : `AuthorizationServiceTest`, `ValidationUseCasesTest`,
`ManagerAbsenceControllerTest`, `PayrollControllerTest`, `AdminControllerTest`,
`AccessDeniedAuditHandlerTest`, `access-guards.spec.ts` (E2E).

### A02 — Cryptographic Failures

- Aucun mot de passe n'est géré par l'application : l'authentification est déléguée à Google OAuth2 /
  OIDC, le jeton JWT est vérifié en Resource Server (signature et émetteur).
- Aucun secret n'est versionné : `.env.example` ne contient que des valeurs de substitution, et
  toutes les variables sensibles (`DB_PASSWORD`, `MAIL_PASSWORD`, `GOOGLE_CLIENT_SECRET`) sont lues
  depuis l'environnement.
- **Action restante** : imposer HTTPS en production au niveau du reverse proxy (hors périmètre du
  code applicatif).

### A03 — Injection

- Tous les accès en base passent par Spring Data JPA avec des requêtes paramétrées (`:param`), y
  compris la recherche RH multi-filtres et l'affectation d'un salarié à une équipe.
- Aucune concaténation de chaîne dans une requête.
- Les entrées HTTP sont validées par Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`), et
  les valeurs d'énumération (rôles, modes de validation, sources d'injection) sont typées.
- Les emails sont produits par des templates Thymeleaf avec échappement par défaut.

### A04 — Insecure Design

- Le workflow de validation est modélisé par des états explicites (`AbsenceVersionStatus`) dont les
  transitions sont contrôlées dans le domaine : une validation depuis un statut interdit lève une
  exception plutôt que de corrompre l'agrégat.
- Le motif de refus (EF-011) et la justification d'annulation manager (EF-013) sont obligatoires au
  niveau du domaine — un contournement de l'interface ne les rend pas facultatifs.
- La clôture de période est non répétable et toute validation ultérieure produit une régularisation
  tracée (EF-025).

### A05 — Security Misconfiguration

- Sessions désactivées (`SessionCreationPolicy.STATELESS`), CSRF désactivé car l'API est sans cookie
  de session et l'authentification repose sur un en-tête `Authorization`.
- Seuls `/actuator/health` et la documentation OpenAPI sont publics ; tout le reste exige
  l'authentification.
- Actuator limité à `health` et `info`, détails de santé réservés aux utilisateurs authentifiés.
- **Action restante** : restreindre l'exposition de Swagger UI en production (profil `prod`).

### A06 — Vulnerable and Outdated Components

- La CI exécute `npm audit --audit-level=high` et `mvn dependency-check:check` (OWASP Dependency
  Check) à chaque exécution.
- PrimeNG est aligné sur Angular 21 (`primeng` 21.1, `@angular/cdk` 21.2). PrimeNG 18, compilé pour
  Angular 18, faisait échouer le rendu de toute l'application (page blanche) ; les tests end-to-end
  le détectent désormais. L'installation garde `--legacy-peer-deps` pour les paquets
  `@angular-eslint` 18, outillage de développement uniquement.

### A07 — Identification and Authentication Failures

- L'identité provient exclusivement du JWT signé par Google ; `CurrentUserProvider` refuse un `sub`
  inconnu de la base.
- Les rôles applicatifs sont chargés depuis la base (`UserRolesLoader`) et non depuis des
  revendications libres du jeton — un jeton ne peut donc pas s'auto-attribuer un rôle.
- Les rôles exposés au frontend proviennent de la base via `GET /api/v1/me` ; le frontend ne lit
  plus de revendication `roles` dans le jeton (un jeton Google n'en porte pas).
- **Tests end-to-end** : un Keycloak jetable remplace Google en CI, par simple changement de
  `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` ; la validation des jetons est identique à
  la production. Les mots de passe de `frontend/e2e/keycloak` ne valent que pour ce Keycloak de
  test et ne sont pas des secrets.

### A08 — Software and Data Integrity Failures

- Les migrations Liquibase sont versionnées, réversibles et vérifiées par un test d'intégration
  (`LiquibaseMigrationIT`).
- Les événements d'audit sont immuables (records Java, écriture seule) et couvrent toutes les
  mutations métier (EF-038).
- Les corrections RH conservent valeur avant et valeur après (EF-023).

### A09 — Security Logging and Monitoring Failures

- Logs JSON structurés en production avec `correlation_id` propagé par `CorrelationIdFilter` (MDC).
- Chaque mutation métier persiste un `EvenementAudit` : auteur, rôle, action, valeurs avant/après,
  `correlation_id`, horodatage.
- Les refus d'accès et les envois d'email sont également journalisés.

### A10 — Server-Side Request Forgery

- L'application n'effectue aucun appel HTTP sortant piloté par une entrée utilisateur. Les seules
  sorties réseau sont le serveur SMTP configuré par variable d'environnement et la récupération des
  clés publiques Google (URL fixe).

---

## Points d'attention pour l'import de fichiers

L'import Excel du module récurrences (`POST /api/v1/recurrences/import`) accepte un fichier fourni par
l'utilisateur. Mesures en place : le fichier n'est jamais écrit sur disque, il est parsé en mémoire par
Apache POI puis rejeté (`IllegalArgumentException` → 400) si illisible, et l'accès au module est
conditionné à `recurrenceEnabled` (EF-030).

**Action restante** : plafonner la taille du fichier accepté
(`spring.servlet.multipart.max-file-size`) avant mise en production, afin d'écarter un déni de
service par fichier volumineux.

---

## Actions restantes avant mise en production

| # | Action | Risque couvert |
|---|--------|----------------|
| 1 | Imposer HTTPS et HSTS au niveau du reverse proxy | A02 |
| 2 | Désactiver Swagger UI sur le profil `prod` | A05 |
| 3 | Plafonner `spring.servlet.multipart.max-file-size` pour l'import Excel | A05 / DoS |
| 4 | ~~Réévaluer PrimeNG dès qu'une version compatible Angular 21 est publiée~~ — fait (PrimeNG 21.1) | A06 |
| 5 | Test d'intrusion externe sur l'environnement de recette | Transverse |

Ces actions relèvent de la configuration d'exploitation ou d'une intervention externe : elles sont
hors du périmètre du code livré et doivent être tracées comme prérequis de mise en production.
