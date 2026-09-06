# Recherche technique — TimePlanning Gestion des Absences

**Phase 0 — Toutes les questions résolues**
**Généré le** : 2026-06-02

---

## 1. Architecture Maven multi-modules DDD hexagonale avec Spring Boot 4.x

**Décision** : 4 modules Maven (`domain`, `application`, `infrastructure`, `api`) + module `frontend` Angular.

**Rationale** :
- Le module `domain` est pur Java sans dépendance Spring → domaine testable unitairement sans contexte Spring.
- L'`application` ne dépend que du `domain` : les ports (interfaces) sont définis ici, les adaptateurs dans `infrastructure`.
- Spring Boot 4.x est compatible Java 25 avec les records Java comme DTOs et les classes scellées pour les états d'absence.
- Le module parent gère le BOM Spring Boot via `spring-boot-starter-parent`.

**Alternatives considérées** :
- Monolithe unique : rejeté car la séparation DDD devient difficile à maintenir sans frontières de modules.
- Architecture microservices : rejetée (over-engineering pour la taille de l'équipe et la volumétrie).

---

## 2. Authentification Google OAuth2 + JWT avec Spring Security Resource Server

**Décision** : Google OAuth2 comme Authorization Server. Le frontend utilise `oidc-client-ts` pour le flux Authorization Code + PKCE. Le backend est un Resource Server Spring Security qui valide les JWT Google.

**Rationale** :
- Spring Security 6.x (inclus dans Spring Boot 4.x) supporte nativement `spring-boot-starter-oauth2-resource-server`.
- La configuration minimale est `spring.security.oauth2.resourceserver.jwt.issuer-uri=https://accounts.google.com`.
- Le `JwtAuthenticationConverter` custom extrait le `sub` Google comme `user_id` et mappe les rôles applicatifs depuis la base locale (Google ne gère pas les rôles métier).
- Le `correlation_id` est injecté dans le MDC via un filtre `OncePerRequestFilter` au démarrage de chaque requête.

**Alternatives considérées** :
- Keycloak comme Authorization Server : plus flexible pour les rôles, mais introduit une infrastructure supplémentaire à maintenir.
- Session côté serveur : rejeté (non compatible avec une SPA stateless).

**Mapping rôles** : La table `user_roles` en base mappe le `sub` Google à un ou plusieurs rôles applicatifs (`SALARIE`, `MANAGER`, `RH`, `ADMIN`). Un utilisateur peut cumuler plusieurs rôles. L'`AuthorizationService` charge le profil à chaque requête (mise en cache possible avec Caffeine).

---

## 3. Calcul des jours ouvrés (EF-002, EF-003, CS-002)

**Décision** : Algorithme interne dans le `domain` — `WorkingDayCalculator` — sans librairie externe.

**Rationale** :
- Les règles sont connues : exclure les samedis, dimanches, jours fériés légaux français, et les jours non travaillés propres au salarié (stockés en base).
- Les jours fériés français sont fixes ou calculables (Pâques, Pentecôte, Ascension). Ils sont pré-générés sur N années et stockés dans une table `public_holidays`.
- Les demi-journées comptent 0,5 jour (représentées par un value object `HalfDayOption`).
- Le calcul enjambant le réveillon/Jour de l'An est couvert par l'itération sur chaque jour calendaire de la plage.

**Alternatives considérées** :
- `ThreeTen-Extra` ou `Jollyday` : rejetés pour éviter des dépendances externes pour une règle métier centrale.

---

## 4. Gestion du versionnement d'absence (EF-007, EF-008, US4)

**Décision** : Pattern **Event Sourcing léger** — l'entité `Absence` est le pivot ; chaque version est une `VersionAbsence` avec son propre statut. Une seule version est marquée `active=true` à un instant T.

**Rationale** :
- La spec exige que l'ancienne version reste la référence active pendant la revalidation.
- La table `absence_version` a une FK vers `absence` + une colonne `version_number` (auto-incrémentée par l'agrégat).
- Le statut de l'`Absence` parente est un agrégat calculé depuis sa version active courante (simplifié pour l'affichage salarié : EN_ATTENTE, VALIDEE, REFUSEE).
- `EvenementAudit` capture chaque transition avec valeur avant/après.

---

## 5. Workflow de double validation N+1 puis N+2 (US3, EF-009, EF-012)

**Décision** : Machine à états dans le domaine avec `WorkflowEngine` — les transitions autorisées dépendent du mode configuré sur l'équipe.

**Statuts internes de `VersionAbsence`** :
```
EN_ATTENTE → VALIDEE_N1 → VALIDEE (workflow double)
EN_ATTENTE → VALIDEE (workflow simple, ou N+2 court-circuite)
EN_ATTENTE → REFUSEE (à tout stade)
VALIDEE → ANNULEE (par manager avec justification)
```

**Rationale** :
- Le salarié ne voit que 3 états simplifiés (EF de la spec US3 scénario 4).
- Le N+2 peut valider directement (EF-012) en passant de `EN_ATTENTE` ou `VALIDEE_N1` vers `VALIDEE`.
- La concurrence simultanée N+1/N+2 est gérée par un verrou optimiste Hibernate (`@Version`) sur `VersionAbsence`.

---

## 6. Export Excel annuel (EF-027, CS-005)

**Décision** : Apache POI (SXSSF — Streaming API) pour la génération streaming sans charger toutes les données en mémoire.

**Rationale** :
- SXSSF est conçu pour les grands volumes (flush après N lignes en mémoire).
- La volumétrie estimée (< 10 000 demandes/an) reste gérable mais on prend la bonne habitude.
- Le format Excel sera défini avec la RH (hypothèse de la spec) ; une interface `PayrollExportTemplate` permet de changer le gabarit sans modifier le service.

---

## 7. Clôture mensuelle et régularisations (US5, EF-024, EF-025)

**Décision** : Table `period_closure` avec statut `OPEN`/`CLOSED`. Un événement de domaine `PeriodClosedEvent` est publié → listener qui identifie les absences validées dans la période et les marque en `REGULARISATION`.

**Rationale** :
- La détection des régularisations est automatique (EF-025) via la FK `absence_version.period_closure_id` alimentée lors de la clôture.
- Toute absence validée après clôture pour une période fermée est signalée sans action manuelle.

---

## 8. Module récurrences (US6, EF-028 à EF-030)

**Décision** : Import Excel (Apache POI) + génération planifiée (Spring `@Scheduled`). Activable par utilisateur via le flag `user_roles.recurrence_enabled`.

**Rationale** :
- L'import Excel réutilise la dépendance POI déjà présente.
- La génération périodique peut être déclenchée manuellement (endpoint `POST /api/v1/recurrences/{id}/generate`) ou par scheduler.
- L'historique d'injection est persisté dans `recurrence_injection_log`.

---

## 9. Tests : JGiven + Allure + MockMvc

**Décision** : Scénarios JGiven mappés 1-pour-1 aux acceptance criteria de la spec (Given/When/Then). Allure pour le reporting HTML. MockMvc pour les tests de contrôleurs. Testcontainers + mariadb4j pour les tests d'intégration JPA.

**Rationale** :
- JGiven produit une documentation vivante lisible par les non-développeurs.
- `@SpringBootTest` + `MockMvc` teste la chaîne complète sans démarrer un vrai serveur HTTP.
- Testcontainers garantit que les tests d'intégration tournent contre un vrai MariaDB (même version que la prod).
- `mariadb4j` est maintenu pour le dev local rapide (`application-dev.properties`).

---

## 10. Traçabilité et corrélation (Principe V)

**Décision** : `CorrelationIdFilter` (`OncePerRequestFilter`) génère ou propage un `X-Correlation-ID` header → MDC Logback. Format de log JSON via `logstash-logback-encoder`.

**Champs obligatoires dans chaque log métier** :
```json
{
  "timestamp": "2026-06-02T10:00:00.000Z",
  "correlation_id": "uuid-v4",
  "user_id": "google-sub-pseudonymisé",
  "action": "ABSENCE_CREATED",
  "resource": "absence/42",
  "result": "SUCCESS"
}
```

**Note RGPD** : le `user_id` dans les logs utilise le `sub` Google (pseudonyme opaque) — aucune donnée nominative dans les logs.

---

## 11. Stratégie de migration Liquibase

**Décision** : Changesets numérotés séquentiellement (`001-create-users.xml`, `002-create-teams.xml`, etc.) dans `infrastructure/src/main/resources/db/changelog/`. Chaque changeset inclut un `rollback` explicite.

**Rationale** :
- Testés en isolation via `liquibase:update` + `liquibase:rollback` dans le pipeline CI.
- Les données de référence (types d'absence par défaut, jours fériés) sont injectées via des changesets dédiés tagués `data`.

---

## Questions résolues — récapitulatif

| Question initiale | Décision finale |
|-------------------|-----------------|
| Format mobile planning/RH ? | Responsive avec PrimeNG DataTable/Calendar ; mobile-first pour les vues salarié et manager, desktop-first pour RH |
| Droits de gestion utilisateurs délégués aux managers ? | Non en V1 — seul l'admin technique gère les utilisateurs (hypothèse spec confirmée) |
| Format exact export paie Excel ? | Interface `PayrollExportTemplate` à définir avec RH avant US5 — colonnes minimales : salarié, équipe, type d'absence, date début, date fin, jours décomptés |
| Concurrence N+1/N+2 simultanée ? | Verrou optimiste Hibernate `@Version` — première action enregistrée prévaut (hypothèse spec confirmée) |
| Justificatifs pièces jointes ? | Hors périmètre V1 (hypothèse spec) |
| Relance automatique validations en attente ? | Hors périmètre V1 (hypothèse spec) |
