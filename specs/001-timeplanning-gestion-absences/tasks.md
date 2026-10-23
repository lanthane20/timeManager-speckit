# Tâches : TimePlanning — Gestion des Absences

**Entrée** : Documents de conception depuis `/specs/001-timeplanning-gestion-absences/`
**Prérequis utilisés** : plan.md ✅ · spec.md ✅ · data-model.md ✅ · contracts/openapi.yaml ✅ · contracts/notifications.md ✅ · research.md ✅ · quickstart.md ✅

**TDD** : Cycle Red-Green-Refactor obligatoire (Constitution Principe III). Les tâches de test sont préfixées 🔴 et **doivent échouer** avant d'écrire l'implémentation.

**Organisation** : Tâches regroupées par user story. Chaque story est un incrément indépendamment livrable.

## Format : `[ID] [P?] [Story?] Description`

- **[P]** : exécutable en parallèle (fichiers différents, pas de dépendance sur une tâche incomplète)
- **[USn]** : user story cible (US1 à US7)
- Chemins relatifs depuis la racine `timeManager/`

---

## Phase 1 : Initialisation du projet

**Objectif** : Structure Maven multi-modules, outillage qualité, configuration d'environnement

- [X] T001 Créer le `pom.xml` parent Maven avec BOM et modules `domain`, `application`, `infrastructure`, `api` dans `pom.xml`
- [X] T002 Créer le `pom.xml` du module `domain` (Java 25, JUnit 5, JGiven, Allure, aucune dépendance Spring) dans `domain/pom.xml`
- [X] T003 [P] Créer le `pom.xml` du module `application` (dépend de `domain`, ports/use cases) dans `application/pom.xml`
- [X] T004 [P] Créer le `pom.xml` du module `infrastructure` (Spring Boot, JPA, Liquibase, Testcontainers, mariadb4j) dans `infrastructure/pom.xml`
- [X] T005 [P] Créer le `pom.xml` du module `api` (Spring Boot Web MVC, Spring Security OAuth2 Resource Server, Springdoc) dans `api/pom.xml`
- [X] T006 Initialiser le projet Angular avec PrimeNG et oidc-client-ts dans `frontend/` via `ng new`
- [X] T007 [P] Configurer Checkstyle (cyclomatique ≤ 10) et Google Java Format dans `config/checkstyle/checkstyle.xml` (partagé par les 4 modules, exécuté en phase `validate`) et le plugin Maven
- [X] T008 [P] Configurer ESLint + Prettier pour le frontend dans `frontend/eslint.config.js` (flat config ESLint 9) et `frontend/.prettierrc`
- [X] T009 Créer `.env.example` avec toutes les variables requises (GOOGLE_CLIENT_ID, DB_URL, MAIL_HOST, NOTIFICATION_ENABLED…) à la racine
- [X] T010 [P] Créer `docker/docker-compose.yml` pour MariaDB dev + `docker/Dockerfile` pour le backend
- [X] T010b [P] Créer le workflow CI GitHub Actions `.github/workflows/ci.yml` avec les étapes : checkout → Java 25 setup → `mvn verify` (lint Checkstyle + tests unitaires + tests intégration Testcontainers) → `npm ci && npm run lint && npm test` → `npm audit` → `mvn dependency:check` (OWASP) → publication rapport Allure en artefact CI

**Point de contrôle** : `mvn validate` réussit — `npm install` dans `frontend/` réussit — premier push sur la branche déclenche le CI sans erreur

---

## Phase 2 : Fondations (prérequis bloquants pour toutes les user stories)

**Objectif** : Infrastructure partagée — sécurité, persistance, audit, auth Angular. **DOIT être complète avant toute user story.**

### Sécurité et authentification

- [X] T011 Créer `SecurityConfig` Spring Security avec validation JWT Google OAuth2 dans `api/src/main/java/fr/timemanager/api/security/SecurityConfig.java`
- [X] T012 [P] Créer `JwtAuthenticationConverter` qui extrait `userId` et rôles depuis le JWT Google dans `infrastructure/src/main/java/fr/timemanager/infrastructure/security/JwtAuthenticationConverter.java`
- [X] T013 [P] 🔴 Écrire les tests JGiven pour `AuthorizationService` (auto-validation interdite, rôles) dans `application/src/test/java/fr/timemanager/application/authorization/AuthorizationServiceTest.java`
- [X] T014 [P] Créer le squelette `AuthorizationService` avec méthodes `assertCannotSelfApprove()`, `assertHasRole()` dans `application/src/main/java/fr/timemanager/application/authorization/AuthorizationService.java`

### Persistance et migrations

- [X] T015 Configurer Liquibase dans `infrastructure/src/main/resources/db/changelog/db.changelog-master.xml`
- [X] T016 Créer le changeset Liquibase `001-users.xml` : tables `users`, `user_roles` dans `infrastructure/src/main/resources/db/changelog/001-users.xml`
- [X] T017 [P] Créer le changeset Liquibase `002-teams.xml` : tables `teams`, `team_members` dans `infrastructure/src/main/resources/db/changelog/002-teams.xml`
- [X] T018 [P] Créer le changeset Liquibase `003-absence-types.xml` : tables `absence_types`, `absence_type_roles` dans `infrastructure/src/main/resources/db/changelog/003-absence-types.xml`
- [X] T019 [P] Créer le changeset Liquibase `004-public-holidays.xml` : table `public_holidays` + seed jours fériés français dans `infrastructure/src/main/resources/db/changelog/004-public-holidays.xml`
- [X] T020 Créer l'entité JPA `UserEntity` + `TeamEntity` + `AbsenceTypeEntity` + leurs repositories dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/`
- [X] T021 [P] 🔴 Écrire le test d'intégration Testcontainers pour les migrations Liquibase (`001`–`004`) dans `infrastructure/src/test/java/fr/timemanager/infrastructure/persistence/LiquibaseMigrationIT.java`

### Traçabilité et audit

- [X] T022 Configurer Logback avec `logstash-logback-encoder` (JSON en prod, texte en dev) et propagation `correlation_id` via MDC dans `api/src/main/resources/logback-spring.xml`
- [X] T023 Créer le filtre HTTP `CorrelationIdFilter` qui injecte `correlation_id` dans MDC à chaque requête dans `api/src/main/java/fr/timemanager/api/security/CorrelationIdFilter.java`
- [X] T024 [P] Créer l'entité domaine `EvenementAudit` (immuable, record Java) dans `domain/src/main/java/fr/timemanager/domain/audit/EvenementAudit.java`
- [X] T025 [P] Créer le port `AuditRepository` (interface) dans `application/src/main/java/fr/timemanager/application/audit/AuditRepository.java`
- [X] T026 [P] Créer le changeset Liquibase `005-audit-events.xml` : table `audit_events` dans `infrastructure/src/main/resources/db/changelog/005-audit-events.xml`
- [X] T027 [P] Créer l'entité JPA `AuditEventEntity` + `AuditEventJpaRepository` dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/audit/`

### Frontend — socle Angular

- [X] T028 Configurer l'authentification OIDC (Google) dans `frontend/src/app/core/auth/auth.service.ts` avec `oidc-client-ts`
- [X] T029 [P] Créer l'intercepteur HTTP `AuthInterceptor` qui ajoute le Bearer token dans `frontend/src/app/core/auth/auth.interceptor.ts`
- [X] T030 [P] Créer les guards Angular `AuthGuard` et `RoleGuard` dans `frontend/src/app/core/auth/guards/`
- [X] T031 [P] Créer le layout principal (shell, navigation, header) dans `frontend/src/app/layout/`
- [X] T032 [P] Créer le service HTTP générique `ApiService` avec gestion des erreurs dans `frontend/src/app/core/http/api.service.ts`

### Entités domaine partagées

- [X] T033 Créer les entités domaine `User`, `Team`, `AbsenceType` avec leurs value objects dans `domain/src/main/java/fr/timemanager/domain/user/`, `domain/.../team/`, `domain/.../absencetype/`
- [X] T034 [P] Créer les value objects partagés `DateRange`, `HalfDay`, `WorkingDays` dans `domain/src/main/java/fr/timemanager/domain/shared/`
- [X] T035 [P] 🔴 Écrire les tests unitaires pour les value objects `DateRange` et `HalfDay` dans `domain/src/test/java/fr/timemanager/domain/shared/`

**Point de contrôle** : `mvn test -pl domain,application` au vert — JWT validé sur un endpoint de test — migrations appliquées sans erreur

---

## Phase 3 : User Story 1 — Salarié : déclarer et suivre une demande d'absence (Priorité : P1) 🎯 MVP

**Objectif** : Un salarié peut créer, consulter, annuler ses demandes d'absence et voir ses compteurs.

**Test indépendant** : Créer une demande avec un salarié test → vérifier son apparition dans "mes demandes" avec statut EN_ATTENTE → l'annuler → vérifier le statut ANNULEE.

### Tests TDD — User Story 1

- [X] T036 [P] 🔴 Écrire les scénarios JGiven pour `CreateAbsenceRequest` (scénarios d'acceptation 1, 3, 4, 5 de US1) dans `application/src/test/java/fr/timemanager/application/absence/CreateAbsenceRequestTest.java`
- [X] T037 [P] 🔴 Écrire les scénarios JGiven pour `CancelAbsenceRequest` (scénario 2 de US1) dans `application/src/test/java/fr/timemanager/application/absence/CancelAbsenceRequestTest.java`
- [X] T038 [P] 🔴 Écrire les tests unitaires pour `WorkingDaysCalculator` (week-ends, jours fériés, demi-journées, changement d'année) dans `domain/src/test/java/fr/timemanager/domain/absence/WorkingDaysCalculatorTest.java`
- [X] T039 [P] 🔴 Écrire les tests unitaires pour `AbsenceOverlapChecker` (chevauchement bloquant vs Ecole non bloquant) dans `domain/src/test/java/fr/timemanager/domain/absence/AbsenceOverlapCheckerTest.java`
- [X] T040 [P] 🔴 Écrire le test MockMvc pour `POST /api/v1/absences` et `DELETE /api/v1/absences/{id}` dans `api/src/test/java/fr/timemanager/api/absence/AbsenceControllerTest.java`
- [X] T041 [P] 🔴 Écrire le test MockMvc pour `GET /api/v1/absences/me` et `GET /api/v1/absences/me/counters` dans `api/src/test/java/fr/timemanager/api/absence/MyAbsencesControllerTest.java` — inclure un scénario négatif EF-019 : vérifier que la réponse des compteurs ne contient aucun champ `solde`, `droitsAcquis` ou `creditDisponible`

### Domaine — User Story 1

- [X] T042 Créer l'agrégat `Absence` + entité `VersionAbsence` + enum `AbsenceVersionStatus` + `AbsenceSimplifiedStatus` dans `domain/src/main/java/fr/timemanager/domain/absence/`
- [X] T043 [P] Créer `WorkingDaysCalculator` (service domaine) dans `domain/src/main/java/fr/timemanager/domain/absence/WorkingDaysCalculator.java`
- [X] T044 [P] Créer `AbsenceOverlapChecker` (service domaine) dans `domain/src/main/java/fr/timemanager/domain/absence/AbsenceOverlapChecker.java`
- [X] T045 [P] Créer l'agrégat calculé `CompteurAbsence` (record Java) dans `domain/src/main/java/fr/timemanager/domain/counter/CompteurAbsence.java`

### Application — User Story 1

- [X] T046 Créer les ports `AbsenceRepository`, `AbsenceTypeRepository`, `PublicHolidayRepository` dans `application/src/main/java/fr/timemanager/application/absence/`
- [X] T047 [P] Implémenter le use case `CreateAbsenceRequest` (EF-001 à EF-005 + audit CREATION) dans `application/src/main/java/fr/timemanager/application/absence/CreateAbsenceRequest.java`
- [X] T048 [P] Implémenter le use case `CancelAbsenceRequest` (EF-006 + audit ANNULATION_SALARIE) dans `application/src/main/java/fr/timemanager/application/absence/CancelAbsenceRequest.java`
- [X] T049 [P] Implémenter le use case `GetMyAbsences` dans `application/src/main/java/fr/timemanager/application/absence/GetMyAbsences.java`
- [X] T050 [P] Implémenter le use case `GetAbsenceCounters` (EF-018, EF-019, EF-020) dans `application/src/main/java/fr/timemanager/application/counter/GetAbsenceCounters.java`
- [X] T051 Créer les DTOs de requête/réponse pour les absences dans `application/src/main/java/fr/timemanager/application/dto/absence/`

### Infrastructure — User Story 1

- [X] T052 Créer les changesets Liquibase `006-absences.xml` et `007-absence-versions.xml` dans `infrastructure/src/main/resources/db/changelog/`
- [X] T053 [P] Créer les entités JPA `AbsenceEntity`, `AbsenceVersionEntity` + `AbsenceJpaRepository`, `AbsenceVersionJpaRepository` dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/absence/`
- [X] T054 [P] Implémenter `AbsenceRepositoryAdapter` (implémente le port domaine) dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/absence/AbsenceRepositoryAdapter.java`
- [X] T055 [P] Implémenter `PublicHolidayRepositoryAdapter` dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/holiday/PublicHolidayRepositoryAdapter.java`
- [X] T056 [P] 🔴 Écrire le test d'intégration Testcontainers pour `AbsenceRepositoryAdapter` dans `infrastructure/src/test/java/fr/timemanager/infrastructure/persistence/absence/AbsenceRepositoryIT.java`

### API — User Story 1

- [X] T057 Créer `AbsenceController` avec `POST /api/v1/absences`, `DELETE /api/v1/absences/{id}`, `GET /api/v1/absences/me`, `GET /api/v1/absences/me/counters` dans `api/src/main/java/fr/timemanager/api/absence/AbsenceController.java`
- [X] T058 [P] Configurer `@PreAuthorize` guards sur les endpoints absence (SALARIE obligatoire) dans `api/src/main/java/fr/timemanager/api/security/`
- [X] T059 [P] Créer le `GlobalExceptionHandler` (400, 403, 404, 409 pour chevauchement) dans `api/src/main/java/fr/timemanager/api/exception/GlobalExceptionHandler.java`

### Frontend — User Story 1

- [X] T060 Créer le module `my-absences` avec le composant liste des demandes en cours dans `frontend/src/app/features/my-absences/`
- [X] T061 [P] Créer le formulaire de création de demande d'absence (type, dates, demi-journées) dans `frontend/src/app/features/my-absences/components/absence-form/`
- [X] T062 [P] Créer le composant compteurs d'absence dans `frontend/src/app/features/counters/`
- [X] T063 [P] Créer le service Angular `AbsenceService` qui appelle les endpoints REST dans `frontend/src/app/features/my-absences/services/absence.service.ts`
- [X] T064 [P] Câbler le routage Angular pour `/my-absences` dans `frontend/src/app/app.routes.ts` (application standalone Angular 21)
- [X] T065 Configurer le port `NotificationService` (interface) dans `application/src/main/java/fr/timemanager/application/notification/NotificationService.java` + adaptateur no-op pour les tests

**Point de contrôle** : Un salarié peut créer, lister, annuler ses absences — compteurs affichés — calcul des jours ouvrés vérifié avec week-ends et jours fériés

---

## Phase 4 : User Story 2 — Manager : valider ou refuser les demandes (Priorité : P1)

**Objectif** : Un manager peut voir les demandes en attente de son périmètre, les valider/refuser, consulter le planning mensuel.

**Test indépendant** : Valider puis refuser des demandes soumises par un salarié test — refus sans motif bloqué — planning mensuel affiché avec couleurs.

### Tests TDD — User Story 2

- [X] T066 [P] 🔴 Écrire les scénarios JGiven pour `ValidateAbsence` et `RefuseAbsence` (scénarios 1, 2, 3 de US2 + EF-010, EF-011) dans `application/src/test/java/fr/timemanager/application/absence/ValidationUseCasesTest.java`
- [X] T066b 🔴 Écrire les scénarios JGiven pour `CancelValidatedAbsence` (EF-013 : justification obligatoire, audit ANNULATION_MANAGER, notification salarié ; test négatif : annulation sans justification → exception) dans `application/src/test/java/fr/timemanager/application/absence/CancelValidatedAbsenceTest.java` — ajouter le test MockMvc pour `DELETE /api/v1/manager/absences/{id}/cancel` (rôle MANAGER périmètre ✅ → 204 ; sans motif → 400 ; rôle SALARIE → 403) dans `api/src/test/java/fr/timemanager/api/manager/ManagerAbsenceControllerTest.java`
- [X] T066c 🔴 Écrire les scénarios JGiven pour `CreateAbsenceForEmployee` (EF-014 : manager délégué saisit une absence pour un salarié de son périmètre ; test négatif : hors périmètre → 403 ; droits non accordés → 403) dans `application/src/test/java/fr/timemanager/application/absence/CreateAbsenceForEmployeeTest.java` — ajouter le test MockMvc pour `POST /api/v1/manager/employees/{employeeId}/absences` (rôle MANAGER périmètre ✅ → 201 ; hors périmètre → 403) dans `api/src/test/java/fr/timemanager/api/manager/ManagerAbsenceControllerTest.java`
- [X] T067 [P] 🔴 Écrire le test MockMvc pour `GET /api/v1/manager/absences/pending`, `PUT /api/v1/manager/absences/{id}/validate`, `PUT /api/v1/manager/absences/{id}/refuse` dans `api/src/test/java/fr/timemanager/api/manager/ManagerAbsenceControllerTest.java`
- [X] T068 [P] 🔴 Écrire le test MockMvc pour `GET /api/v1/planning` dans `api/src/test/java/fr/timemanager/api/planning/PlanningControllerTest.java`

### Application — User Story 2

- [X] T069 Implémenter le use case `ValidateAbsence` (workflow N1_ONLY — statut VALIDEE + audit VALIDATION_DIRECTE + notification salarié) dans `application/src/main/java/fr/timemanager/application/absence/ValidateAbsence.java`
- [X] T070 [P] Implémenter le use case `RefuseAbsence` (motif obligatoire EF-011 + audit REFUS + notification salarié) dans `application/src/main/java/fr/timemanager/application/absence/RefuseAbsence.java`
- [X] T071 [P] Implémenter le use case `CancelValidatedAbsence` (manager + justification obligatoire EF-013 + audit ANNULATION_MANAGER) dans `application/src/main/java/fr/timemanager/application/absence/CancelValidatedAbsence.java`
- [X] T072 [P] Implémenter le use case `GetPendingAbsencesForManager` (périmètre équipe manager) dans `application/src/main/java/fr/timemanager/application/absence/GetPendingAbsencesForManager.java`
- [X] T073 [P] Implémenter le use case `CreateAbsenceForEmployee` (EF-014 — saisie par manager délégué) dans `application/src/main/java/fr/timemanager/application/absence/CreateAbsenceForEmployee.java`
- [X] T074 [P] Implémenter le use case `GetMonthlyPlanning` (EF-015, EF-016, EF-017) dans `application/src/main/java/fr/timemanager/application/planning/GetMonthlyPlanning.java`
- [X] T075 [P] Renforcer `AuthorizationService` : `assertIsManagerOf()`, `assertCannotSelfApprove()` dans `application/src/main/java/fr/timemanager/application/authorization/AuthorizationService.java`
- [X] T075b 🔴 Écrire le test MockMvc pour `GET /api/v1/manager/employees/{employeeId}/counters` (rôle MANAGER périmètre ✅ → 200 ; hors périmètre → 403 ; rôle SALARIE → 403) dans `api/src/test/java/fr/timemanager/api/manager/ManagerAbsenceControllerTest.java`
- [X] T075c Ajouter l'endpoint `GET /api/v1/manager/employees/{employeeId}/counters` dans `ManagerAbsenceController` — délègue à `GetAbsenceCounters` avec vérification `assertIsManagerOf()` dans `api/src/main/java/fr/timemanager/api/absence/ManagerAbsenceController.java`

### Infrastructure — User Story 2

- [X] T076 Implémenter `EmailNotificationService` avec `JavaMailSender` (désactivable via `NOTIFICATION_ENABLED`) dans `infrastructure/src/main/java/fr/timemanager/infrastructure/notification/EmailNotificationService.java`
- [X] T077 [P] Créer les templates email (création, validation, refus, annulation, modification) dans `infrastructure/src/main/resources/templates/email/`

### API — User Story 2

- [X] T078 Créer `ManagerAbsenceController` avec `GET /api/v1/manager/absences/pending`, `PUT .../validate`, `PUT .../refuse`, `DELETE .../cancel` dans `api/src/main/java/fr/timemanager/api/absence/ManagerAbsenceController.java` — tests dans `api/src/test/java/fr/timemanager/api/manager/ManagerAbsenceControllerTest.java`
- [X] T079 [P] Créer `PlanningController` avec `GET /api/v1/planning` (filtres mois, équipe, sous-équipe) dans `api/src/main/java/fr/timemanager/api/planning/PlanningController.java`
- [X] T080 [P] Configurer `@PreAuthorize(MANAGER)` sur tous les endpoints manager dans `api/src/main/java/fr/timemanager/api/security/`
- [X] T080b [P] 🔴 Écrire le test MockMvc vérifiant qu'une réponse 403 génère un événement d'audit `ACCES_REFUSE` avec `user_id`, `resource` (endpoint), `result=DENIED` dans `api/src/test/java/fr/timemanager/api/security/AccessDeniedAuditHandlerTest.java`
- [X] T080c [P] Implémenter `AccessDeniedAuditHandler` Spring Security (`AccessDeniedHandler`) qui persiste un `EvenementAudit` de type `ACCES_REFUSE` sur chaque 403 dans `api/src/main/java/fr/timemanager/api/security/AccessDeniedAuditHandler.java`

### Frontend — User Story 2

- [X] T081 Créer le module `validation` avec la liste des demandes en attente et les actions valider/refuser dans `frontend/src/app/features/validation/`
- [X] T082 [P] Créer le composant planning mensuel avec PrimeNG Calendar/Table et affichage coloré des absences dans `frontend/src/app/features/planning/`
- [X] T083 [P] Créer les services Angular `ManagerAbsenceService` et `PlanningService` dans `frontend/src/app/features/validation/services/` et `frontend/src/app/features/planning/services/`
- [X] T084 [P] Câbler le routage Angular pour `/validation` et `/planning` dans `frontend/src/app/app.routes.ts` (guards de rôle MANAGER / RH)
- [X] T085 Implémenter le feedback visuel ≤ 200 ms (spinner PrimeNG) pour toutes les actions manager dans les composants validation

**Point de contrôle** : Un manager peut traiter une demande en attente — refus sans motif bloqué en UI et API — planning mensuel visible avec filtres équipe

---

## Phase 5 : User Story 3 — Workflow N+1 puis N+2 (Priorité : P2)

**Objectif** : Double validation configurable par équipe, N+2 peut court-circuiter, salarié ne voit qu'un statut simplifié.

**Test indépendant** : Configurer une équipe en N1_AND_N2 → soumettre une demande → vérifier la transition VALIDEE_N1 → valider par N+2 → VALIDEE.

### Tests TDD — User Story 3

- [X] T086 [P] 🔴 Écrire les scénarios JGiven pour le workflow double validation (scénarios 1 à 4 de US3 + EF-012) dans `application/src/test/java/fr/timemanager/application/absence/DoubleValidationWorkflowTest.java`
- [X] T087 [P] 🔴 Écrire le test MockMvc pour `PUT /api/v1/manager/absences/{id}/validate-n2` dans `api/src/test/java/fr/timemanager/api/manager/N2ValidationControllerTest.java`

### Application — User Story 3

- [X] T088 Créer `WorkflowEngine` qui route selon `ValidationMode` (N1_ONLY vs N1_AND_N2) dans `application/src/main/java/fr/timemanager/application/absence/WorkflowEngine.java`
- [X] T089 [P] Étendre `ValidateAbsence` pour gérer la transition EN_ATTENTE → VALIDEE_N1 (workflow N1_AND_N2 + audit VALIDATION_N1 + notification N+2) dans `application/src/main/java/fr/timemanager/application/absence/ValidateAbsence.java`
- [X] T090 [P] Implémenter le use case `ValidateAbsenceN2` (EF-012 : N+2 direct ou après N+1 + audit VALIDATION_N2) dans `application/src/main/java/fr/timemanager/application/absence/ValidateAbsenceN2.java`
- [X] T091 [P] Mettre à jour la logique de notification (EF-037 : seul le N+1 reçoit l'email initial) dans `application/src/main/java/fr/timemanager/application/notification/`

### API — User Story 3

- [X] T092 Ajouter `PUT /api/v1/manager/absences/{id}/validate-n2` dans `ManagerAbsenceController.java`
- [X] T093 [P] Configurer le guard `@PreAuthorize` N+2 (doit être N+2 de l'équipe concernée) dans `api/src/main/java/fr/timemanager/api/security/`

### Frontend — User Story 3

- [X] T094 Mettre à jour le module `validation` pour afficher les files N+1 et N+2 séparément (onglets, endpoint `GET /api/v1/manager/absences/pending-n2`) dans `frontend/src/app/features/validation/`
- [X] T095 [P] S'assurer que la vue salarié n'affiche que le statut simplifié (EN_ATTENTE / VALIDEE / REFUSEE) dans `frontend/src/app/features/my-absences/`

**Point de contrôle** : Workflow double validation fonctionnel — statut simplifié côté salarié — N+2 peut court-circuiter N+1

---

## Phase 6 : User Story 4 — Salarié : modifier une absence validée (Priorité : P2)

**Objectif** : La modification d'une absence validée crée une nouvelle version qui repasse en validation ; l'ancienne version reste active en attendant.

**Test indépendant** : Modifier une absence validée → vérifier que l'ancienne version reste VALIDEE et active → valider la nouvelle → vérifier que l'ancienne est archivée.

### Tests TDD — User Story 4

- [X] T096 [P] 🔴 Écrire les scénarios JGiven pour `RequestAbsenceModification` (scénarios 1 à 3 de US4 + EF-007, EF-008) dans `application/src/test/java/fr/timemanager/application/absence/AbsenceModificationTest.java`
- [X] T097 [P] 🔴 Écrire le test MockMvc pour `PUT /api/v1/absences/{id}/modify` dans `api/src/test/java/fr/timemanager/api/absence/AbsenceModificationControllerTest.java`

### Application — User Story 4

- [X] T098 Implémenter le use case `RequestAbsenceModification` (EF-007 : crée VersionAbsence v+1 EN_ATTENTE, ancienne reste active + audit MODIFICATION_DEMANDEE) dans `application/src/main/java/fr/timemanager/application/absence/RequestAbsenceModification.java`
- [X] T099 [P] Étendre `ValidateAbsence` et `ValidateAbsenceN2` pour archiver l'ancienne version quand la nouvelle est validée (EF-008) dans `application/src/main/java/fr/timemanager/application/absence/`

### Infrastructure — User Story 4

- [X] T100 [P] **N/A — aucun changeset supplémentaire nécessaire.** Le schéma `absence_versions` (changeset `007`) couvre déjà la gestion des versions multiples : `version_number` ordonne la chaîne de versions, `active` désigne la version de référence (EF-008) et `status` porte l'état de revalidation. Un `previous_version_id` serait redondant avec `version_number` et un `archived_at` avec la transition `active = false` déjà tracée par l'audit `MODIFICATION_DEMANDEE`. Ajouter ces colonnes serait une abstraction non nécessaire (Constitution Principe I — YAGNI)

### API — User Story 4

- [X] T101 Ajouter `PUT /api/v1/absences/{id}/modify` dans `AbsenceController.java`

### Frontend — User Story 4

- [X] T102 Ajouter le bouton « Modifier » sur les absences validées dans `frontend/src/app/features/my-absences/`
- [X] T103 [P] Réutiliser le formulaire de création pour la modification (pré-remplissage + indicateur "version en cours de revalidation") dans `frontend/src/app/features/my-absences/components/absence-form/`

**Point de contrôle** : Modification d'absence validée fonctionnelle — ancienne version reste active pendant revalidation — archivage correct après approbation

---

## Phase 7 : User Story 5 — RH : clôture mensuelle et export paie (Priorité : P2)

**Objectif** : RH peut clôturer un mois, voir les régularisations automatiques, générer l'export Excel annuel, corriger des absences.

**Test indépendant** : Clôturer un mois → valider une absence rétroactivement → vérifier la régularisation générée → générer l'export Excel et vérifier sa structure.

### Tests TDD — User Story 5

- [X] T104 [P] 🔴 Écrire les scénarios JGiven pour `CloseMonth` (scénarios 1, 2 de US5 + EF-024, EF-025) dans `application/src/test/java/fr/timemanager/application/payroll/CloseMonthTest.java`
- [X] T105 [P] 🔴 Écrire les scénarios JGiven pour `GeneratePayrollExport` (scénario 3 de US5 + EF-027, CS-005) dans `application/src/test/java/fr/timemanager/application/payroll/GeneratePayrollExportTest.java`
- [X] T106 [P] 🔴 Écrire le test MockMvc pour `POST /api/v1/rh/periods/{year}/{month}/close`, `GET /api/v1/rh/regularisations`, `GET /api/v1/rh/export/annual/{year}` dans `api/src/test/java/fr/timemanager/api/payroll/PayrollControllerTest.java`

### Domaine — User Story 5

- [X] T107 Créer les entités domaine `PeriodeCloture` et `Regularisation` dans `domain/src/main/java/fr/timemanager/domain/payroll/`

### Application — User Story 5

- [X] T108 Implémenter le use case `CloseMonth` (EF-024 : fige la période + EF-025 : détecte les régularisations + audit CLOTURE_PERIODE) dans `application/src/main/java/fr/timemanager/application/payroll/CloseMonth.java`
- [X] T109 [P] Implémenter le use case `GetRegularisations` (EF-026 — filtre par période) dans `application/src/main/java/fr/timemanager/application/payroll/GetRegularisations.java`
- [X] T109b [P] 🔴 Écrire les scénarios JGiven pour `SearchAbsencesForHR` (EF-021 — filtres : salarié, équipe, type d'absence, période ; seul le rôle RH peut appeler ce use case) dans `application/src/test/java/fr/timemanager/application/payroll/SearchAbsencesForHRTest.java`
- [X] T109c [P] Implémenter le use case `SearchAbsencesForHR` (EF-021) dans `application/src/main/java/fr/timemanager/application/payroll/SearchAbsencesForHR.java`
- [X] T109d [P] 🔴 Écrire le test MockMvc pour `GET /api/v1/rh/absences?employeeId=&teamId=&typeId=&from=&to=` (pagination incluse ; rôle SALARIE → 403) dans `api/src/test/java/fr/timemanager/api/payroll/PayrollControllerTest.java`
- [X] T109e [P] Ajouter l'endpoint `GET /api/v1/rh/employees/{employeeId}/counters` dans `PayrollController` — délègue à `GetAbsenceCounters` avec guard `@PreAuthorize(RH)` dans `api/src/main/java/fr/timemanager/api/payroll/PayrollController.java`
- [X] T110 [P] Implémenter le use case `GeneratePayrollExport` (EF-027 — délègue au port `ExcelExportPort`) dans `application/src/main/java/fr/timemanager/application/payroll/GeneratePayrollExport.java`
- [X] T111 [P] Créer le port `ExcelExportPort` (interface) dans `application/src/main/java/fr/timemanager/application/payroll/ExcelExportPort.java`
- [X] T112 [P] Implémenter le use case `CorrectAbsence` (EF-023 — correction RH + audit CORRECTION_RH avec valueBefore/valueAfter) dans `application/src/main/java/fr/timemanager/application/payroll/CorrectAbsence.java`
- [X] T112b [P] 🔴 Écrire les scénarios JGiven pour `CreateSpecialAbsenceByHR` (EF-022 — arrêt maladie, congé maternité, congé spécial pour tout salarié, avec audit CREATION_RH) dans `application/src/test/java/fr/timemanager/application/payroll/CreateSpecialAbsenceByHRTest.java`
- [X] T112c [P] Implémenter le use case `CreateSpecialAbsenceByHR` (EF-022 — seul le rôle RH peut appeler ce use case ; délègue à `CreateAbsenceRequest` avec contournement des règles manager + audit CREATION_RH) dans `application/src/main/java/fr/timemanager/application/payroll/CreateSpecialAbsenceByHR.java`
- [X] T112d [P] 🔴 Écrire le test MockMvc pour `POST /api/v1/rh/absences` (rôle SALARIE → 403, rôle MANAGER → 403, rôle RH → 201) dans `api/src/test/java/fr/timemanager/api/payroll/PayrollControllerTest.java`

### Infrastructure — User Story 5

- [X] T113 Créer les changesets Liquibase `009-period-closures.xml` et `010-regularisations.xml` dans `infrastructure/src/main/resources/db/changelog/` (déjà présents et inclus dans `db.changelog-master.xml`)
- [X] T114 [P] Créer les entités JPA + repositories `PeriodClosureEntity`, `RegularisationEntity` et leurs adaptateurs dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/payroll/`
- [X] T115 [P] Implémenter `ExcelExportAdapter` avec Apache POI (structure par salarié/type/période, < 30 s CS-005) dans `infrastructure/src/main/java/fr/timemanager/infrastructure/export/ExcelExportAdapter.java`

### API — User Story 5

- [X] T116 Créer `PayrollController` avec `POST /api/v1/rh/periods/{year}/{month}/close`, `GET /api/v1/rh/regularisations`, `GET /api/v1/rh/export/annual/{year}`, `PUT /api/v1/rh/absences/{id}/correct`, `GET /api/v1/rh/absences` (EF-021), `POST /api/v1/rh/absences` (EF-022), `GET /api/v1/rh/employees/{employeeId}/counters` (EF-020) dans `api/src/main/java/fr/timemanager/api/payroll/PayrollController.java`
- [X] T117 [P] Configurer `@PreAuthorize(RH)` sur tous les endpoints payroll dans `api/src/main/java/fr/timemanager/api/security/`

### Frontend — User Story 5

- [X] T118 Créer le module `payroll` dans `frontend/src/app/features/payroll/` avec :
  - tableau de bord RH (clôtures, régularisations, bouton export)
  - formulaire de saisie d'absence spéciale (EF-022 — type, salarié cible, dates, demi-journées)
  - formulaire de correction d'absence existante (EF-023)
  - vue liste des absences avec filtres (EF-021 — salarié, équipe, type, période)

**Point de contrôle** : Clôture mensuelle fonctionnelle — régularisation détectée automatiquement — export Excel téléchargeable en < 30 s

---

## Phase 8 : User Story 6 — Module récurrences : Ecole et jours non travaillés (Priorité : P3)

**Objectif** : Créer/importer/générer des absences récurrentes (alternants, contrats spéciaux) avec historique d'injections.

**Test indépendant** : Importer un fichier Excel de planning école → vérifier la création des absences Ecole sans blocage sur chevauchements → consulter l'historique des injections.

### Tests TDD — User Story 6

- [X] T119 [P] 🔴 Écrire les scénarios JGiven pour `InjectRecurrencePattern` (scénarios 1 à 4 de US6 + EF-028, EF-029, EF-030) dans `application/src/test/java/fr/timemanager/application/recurrence/InjectRecurrencePatternTest.java`
- [X] T120 [P] 🔴 Écrire le test MockMvc pour `POST /api/v1/recurrences/import`, `POST /api/v1/recurrences/generate`, `GET /api/v1/recurrences/history` dans `api/src/test/java/fr/timemanager/api/recurrence/RecurrenceControllerTest.java`

### Domaine — User Story 6

- [X] T121 Créer les entités domaine `RecurrencePattern` et `RecurrenceInjectionLog` dans `domain/src/main/java/fr/timemanager/domain/recurrence/`

### Application — User Story 6

- [X] T122 Implémenter le use case `InjectRecurrencePattern` (saisie unitaire, génération périodique — EF-028, EF-030 : vérifier `recurrenceEnabled`) dans `application/src/main/java/fr/timemanager/application/recurrence/InjectRecurrencePattern.java`
- [X] T123 [P] Implémenter le use case `ImportRecurrenceFromExcel` (EF-028 — délègue à `ExcelImportPort`) dans `application/src/main/java/fr/timemanager/application/recurrence/ImportRecurrenceFromExcel.java`
- [X] T124 [P] Créer le port `ExcelImportPort` (interface) dans `application/src/main/java/fr/timemanager/application/recurrence/ExcelImportPort.java`
- [X] T125 [P] Implémenter le use case `GetRecurrenceHistory` (EF-029) dans `application/src/main/java/fr/timemanager/application/recurrence/GetRecurrenceHistory.java`

### Infrastructure — User Story 6

- [X] T126 Créer les changesets Liquibase `011-recurrence-patterns.xml` et `012-recurrence-injection-logs.xml` dans `infrastructure/src/main/resources/db/changelog/` (déjà présents et inclus dans `db.changelog-master.xml`)
- [X] T127 [P] Créer les entités JPA + repositories dans `infrastructure/src/main/java/fr/timemanager/infrastructure/persistence/recurrence/`
- [X] T128 [P] Implémenter `ExcelImportAdapter` avec Apache POI pour le parsing du planning école dans `infrastructure/src/main/java/fr/timemanager/infrastructure/recurrence/ExcelImportAdapter.java`

### API + Frontend — User Story 6

- [X] T129 Créer `RecurrenceController` avec `POST /api/v1/recurrences/import` (multipart), `POST /api/v1/recurrences/generate`, `GET /api/v1/recurrences/history` dans `api/src/main/java/fr/timemanager/api/recurrence/RecurrenceController.java`
- [X] T130 [P] Créer le module `recurrence` Angular (formulaire saisie, upload Excel, tableau historique) dans `frontend/src/app/features/recurrence/`

**Point de contrôle** : Import Excel école → absences Ecole créées sans blocage — historique d'injections visible — module accessible uniquement aux utilisateurs avec `recurrenceEnabled=true`

---

## Phase 9 : User Story 7 — Admin technique : paramétrage (Priorité : P3)

**Objectif** : Gestion des utilisateurs, équipes, hiérarchie N+1/N+2, types d'absence — sans accès aux données individuelles.

**Test indépendant** : Créer un nouveau type d'absence en admin → vérifier qu'il apparaît dans les options de création d'absence d'un salarié.

### Tests TDD — User Story 7

- [X] T131 [P] 🔴 Écrire les scénarios JGiven pour `ManageUsers`, `ManageTeams`, `ManageAbsenceTypes` (scénarios 1 à 3 de US7 + EF-031 à EF-035) dans `application/src/test/java/fr/timemanager/application/admin/AdminUseCasesTest.java`
- [X] T132 [P] 🔴 Écrire le test MockMvc pour les endpoints `/api/v1/admin/users`, `/api/v1/admin/teams`, `/api/v1/admin/absence-types` dans `api/src/test/java/fr/timemanager/api/admin/AdminControllerTest.java`

### Application — User Story 7

- [X] T133 Implémenter le use case `ManageUsers` (EF-031 : CRUD + affectation équipe/rôle) dans `application/src/main/java/fr/timemanager/application/admin/ManageUsers.java`
- [X] T133b [P] 🔴 Écrire les tests unitaires pour la lecture des jours non travaillés contractuels dans `WorkingDaysCalculator` (EF-031b — ex. salarié à 4/5 → mercredi exclu ; date d'effet future → non appliquée) dans `domain/src/test/java/fr/timemanager/domain/absence/WorkingDaysCalculatorContractTest.java`
- [X] T133c [P] Étendre `ManageUsers` pour inclure le CRUD du planning contractuel (EF-031b) : créer l'entité domaine `WorkSchedule` dans `domain/src/main/java/fr/timemanager/domain/user/WorkSchedule.java` + changeset Liquibase `013-work-schedules.xml` dans `infrastructure/src/main/resources/db/changelog/013-work-schedules.xml`
- [X] T134 [P] Implémenter le use case `ManageTeams` (EF-032, EF-033 : CRUD équipes, validation mode, hiérarchie N+1/N+2) dans `application/src/main/java/fr/timemanager/application/admin/ManageTeams.java`
- [X] T135 [P] Implémenter le use case `ManageAbsenceTypes` (EF-034 : CRUD complet, statut actif/inactif) dans `application/src/main/java/fr/timemanager/application/admin/ManageAbsenceTypes.java`
- [X] T135b 🔴 Écrire les tests unitaires pour le filtre `resetAnnuel` dans `GetAbsenceCounters` (EF-034) — scénarios : type avec `resetAnnuel=true` → seules les absences de l'année civile courante comptent ; type avec `resetAnnuel=false` → toutes les absences comptent ; absence à cheval sur le Nouvel An → imputée à l'année de la date de début dans `application/src/test/java/fr/timemanager/application/counter/GetAbsenceCountersResetAnnuelTest.java`
- [X] T135c [P] Étendre `GetAbsenceCounters` pour appliquer un filtre par année civile courante quand `TypeAbsence.resetAnnuel=true` (EF-034) dans `application/src/main/java/fr/timemanager/application/counter/GetAbsenceCounters.java` — **aucun état persistant, aucun job, aucune table supplémentaire** : `CompteurAbsence` étant calculé dynamiquement (spec §Entités clés), la "remise à zéro annuelle" est un simple filtre de date dans la requête de calcul
- [X] T136 [P] Ajouter dans `AuthorizationService` les règles `assertAdminHasNoAbsenceDataAccess()` (EF-035) et `assertAdminCannotAccessCounters()` (EF-020 — les compteurs sont des données dérivées d'absences) dans `application/src/main/java/fr/timemanager/application/authorization/AuthorizationService.java`

### API — User Story 7

- [X] T137 Créer `AdminController` avec CRUD pour `/api/v1/admin/users`, `/api/v1/admin/teams`, `/api/v1/admin/absence-types` dans `api/src/main/java/fr/timemanager/api/admin/AdminController.java`
- [X] T138 [P] Configurer `@PreAuthorize(ADMIN)` + vérification que les endpoints admin n'exposent aucune donnée d'absence dans `api/src/main/java/fr/timemanager/api/security/`

### Frontend — User Story 7

- [X] T139 Créer le module `admin` avec panneaux de gestion utilisateurs, équipes, types d'absence dans `frontend/src/app/features/admin/`
- [X] T140 [P] Câbler le routage `/admin` avec `RoleGuard(ADMIN)` dans `frontend/src/app/app.routes.ts`

**Point de contrôle** : Admin peut créer un type d'absence et le voir disponible en saisie — admin ne peut pas accéder aux absences individuelles

---

## Phase 10 : Finalisation & préoccupations transversales

**Objectif** : Documentation API, sécurité renforcée, accessibilité, validation du quickstart.

- [X] T141 [P] Configurer Springdoc OpenAPI et annoter tous les controllers pour générer `openapi.yaml` auto à `/v3/api-docs` dans `api/src/main/java/fr/timemanager/api/openapi/OpenApiConfig.java`
- [X] T141b Configurer Playwright dans `frontend/e2e/` : installation, configuration CI, page object pattern de base dans `frontend/e2e/playwright.config.ts`
- [X] T141c [P] 🔴 Écrire le test E2E flux US1→US2 : salarié crée une absence → manager la valide → vérifier statut "Validée" dans les deux vues dans `frontend/e2e/tests/absence-validation.spec.ts`
- [X] T141d [P] 🔴 Écrire le test E2E flux US5 : RH clôture un mois → déclenche l'export Excel → vérifier que le téléchargement s'amorce dans `frontend/e2e/tests/payroll-export.spec.ts`
- [X] T141e [P] 🔴 Écrire le test E2E guard accès : vérifier qu'un salarié ne peut pas accéder à `/admin`, `/validation`, ni `/rh` dans `frontend/e2e/tests/access-guards.spec.ts`
- [X] T142 [P] Vérifier la conformité WCAG 2.1 AA sur les composants PrimeNG (contraste, navigation clavier, ARIA) dans `frontend/src/app/features/`
- [X] T143 [P] Ajouter les tests de performance (temps réponse API p95 < 200 ms CS-001, export < 30 s CS-005) dans `api/src/test/java/fr/timemanager/api/performance/`
- [X] T144 [P] Effectuer une revue OWASP Top 10 sur tous les nouveaux endpoints (IDOR, injection, CSRF) — documenter dans `specs/001-timeplanning-gestion-absences/security-review.md`
- [X] T145 [P] Valider et mettre à jour `quickstart.md` en déroulant les étapes une par une dans `specs/001-timeplanning-gestion-absences/quickstart.md` — 8 commandes corrigées (wrapper `./mvnw`, `--legacy-peer-deps`, Vitest, Spotless, goals Liquibase inexistants)
- [X] T146 Vérifier que `.env.example` est synchronisé avec toutes les variables utilisées dans le code à la racine — écart corrigé : `DB_*`, `MAIL_*` et `NOTIFICATION_ENABLED` n'étaient reliées à aucune propriété Spring ; ajout de `NOTIFICATION_FROM` et `APP_BASE_URL`
- [X] T147 [P] Générer le rapport Allure complet et vérifier que tous les scénarios JGiven sont au vert
  - Écart corrigé : les résultats Allure étaient écrits à la racine des modules au lieu de `target/`, et `reportVersion` pointait sur la version de la bibliothèque `allure-junit5` (2.29.1), qui n'existe pas en ligne de commande — la génération échouait systématiquement. Version d'outil désormais distincte (`allure-commandline.version` = 2.46.1).
  - Rapport généré : **161 tests, 0 échec** (domaine 24 · application 74 · api 63).
  - Tests d'intégration Testcontainers (MariaDB) exécutés en CI et en local avec Docker : **12 tests, 0 échec**. Leur première exécution a révélé et fait corriger (PR #2) l'incompatibilité de Testcontainers 1.21.2 avec Docker 29, l'écart de types `CHAR(36)` entre Liquibase et les entités JPA (validation Hibernate), la clé étrangère circulaire `absences.current_version_id` lors de l'enregistrement d'une absence et une fixture de test incomplète.
  - Total : **173 tests, 0 échec** (domaine 24 · application 74 · infrastructure 12 · api 63).
- [ ] T148 ⛔ **Bloqué — nécessite des participants humains.** Le protocole prêt à dérouler et les tableaux de mesure sont rédigés dans [`ux-validation.md`](./ux-validation.md) ; seule la session avec 3 managers et 1 admin reste à mener. Valider les critères de succès UX non automatisables — effectuer un test utilisateur manuel avec 3 managers ET 1 admin représentatifs et documenter les résultats dans `specs/001-timeplanning-gestion-absences/ux-validation.md` :
  - **CS-001** : mesurer le temps réel de création d'une demande d'absence (objectif : < 2 min sans formation)
  - **CS-003** : mesurer le temps de traitement d'une demande en attente par un manager (objectif : < 1 min depuis la notification)
  - **CS-008** : mesurer le temps de création d'un type d'absence depuis le module admin (objectif : < 5 min)
  - **CS-009** : vérifier que 90 % des participants comprennent l'état des absences de leur équipe au premier coup d'œil sur le planning mensuel

---

## Dépendances & ordre d'exécution

### Dépendances entre phases

- **Phase 1** : Aucune dépendance — démarre immédiatement
- **Phase 2** : Dépend de la Phase 1 — **bloque toutes les user stories**
- **Phase 3 (US1)** et **Phase 4 (US2)** : Peuvent démarrer en parallèle après la Phase 2 (équipes distinctes)
- **Phase 5 (US3)** : Dépend de Phase 4 (étend le workflow de validation)
- **Phase 6 (US4)** : Dépend de Phase 3 (étend les versions d'absence)
- **Phase 7 (US5)** : Peut démarrer après la Phase 2 — données d'absence nécessaires (US1 recommandée)
- **Phase 8 (US6)** : Dépend de Phase 3 (crée des absences de type Ecole)
- **Phase 9 (US7)** : Peut démarrer après la Phase 2 en parallèle des US
- **Phase 10** : Dépend de toutes les US souhaitées

### Dépendances entre user stories

| Story | Dépend de | Peut démarrer en parallèle avec |
|-------|-----------|--------------------------------|
| US1 (P1) | Phase 2 | US2, US7 |
| US2 (P1) | Phase 2 | US1, US7 |
| US3 (P2) | US2 | US4, US5, US6 |
| US4 (P2) | US1 + US3 | US5, US6 |
| US5 (P2) | Phase 2 (+ US1 recommandé) | US3, US4, US6 |
| US6 (P3) | US1 | US7 |
| US7 (P3) | Phase 2 | US1, US2 |

### Opportunités de parallélisation par phase

```
Phase 1 : T002–T010 tous parallélisables après T001
Phase 2 : T011–T014 (sécurité) ‖ T015–T021 (migrations) ‖ T022–T027 (audit) ‖ T028–T032 (frontend)
Phase 3 : T036–T041 (tests) → T042–T045 (domaine) ‖ T046–T051 (application) → T052–T056 (infra) ‖ T057–T059 (API) ‖ T060–T065 (frontend)
```

---

## Exemple de parallélisation : User Story 1

```
# Étape 1 — Écrire les tests en parallèle (tous en parallèle)
T036 : JGiven CreateAbsenceRequest
T037 : JGiven CancelAbsenceRequest
T038 : Test WorkingDaysCalculator
T039 : Test AbsenceOverlapChecker
T040 : MockMvc POST/DELETE /absences
T041 : MockMvc GET /absences/me + /counters

# Étape 2 — Implémenter le domaine en parallèle
T042 : Agrégat Absence + VersionAbsence
T043 : WorkingDaysCalculator
T044 : AbsenceOverlapChecker
T045 : CompteurAbsence

# Étape 3 — Application + Infrastructure en parallèle
T046 : Ports (repository interfaces)
T047 : CreateAbsenceRequest ‖ T048 : CancelAbsenceRequest ‖ T049 : GetMyAbsences ‖ T050 : GetAbsenceCounters

# Étape 4 — Infrastructure en parallèle
T052 : Migrations ‖ T053 : Entités JPA ‖ T054 : AbsenceRepositoryAdapter ‖ T055 : PublicHolidayRepositoryAdapter

# Étape 5 — API + Frontend en parallèle
T057 : AbsenceController ‖ T060 : Module my-absences ‖ T062 : Compteurs ‖ T063 : AbsenceService Angular
```

---

## Stratégie d'implémentation

### MVP Phase 1 (User Stories 1 + 2 uniquement)

1. Phase 1 : Initialisation
2. Phase 2 : Fondations — **CRITIQUE**
3. Phase 3 : US1 — Salarié déclare ses absences
4. **Pause** : tester US1 indépendamment
5. Phase 4 : US2 — Manager valide les demandes
6. **Pause** : tester le flux complet US1 → US2
7. **Démo / déploiement pilote**

### Livraison incrémentale complète

1. Init + Fondations → **socle prêt**
2. US1 + US2 → **MVP fonctionnel** (flux quotidien)
3. US3 + US4 → **workflows avancés**
4. US5 → **RH/Paie opérationnelle**
5. US6 + US7 → **administration complète**

### Stratégie équipe en parallèle (après Phase 2)

- **Dev A** : US1 (Salarié)
- **Dev B** : US2 (Manager)
- **Dev C** : US7 (Admin) — pas de dépendance sur US1/US2

---

## Notes

- 🔴 = écrire le test **avant** l'implémentation, vérifier qu'il **échoue** (Red)
- [P] = fichiers différents, pas de dépendances non terminées — exécutable en parallèle
- [USn] = relie la tâche à une user story pour la traçabilité et le découpage en sprints
- Chaque user story doit être indépendamment livrable et démo-able
- Committer après chaque tâche ou groupe logique cohérent
- S'arrêter à chaque **Point de contrôle** pour valider la story de façon isolée avant de passer à la suivante
