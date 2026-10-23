# Plan d'implémentation : TimePlanning — Gestion des Absences

**Branche** : `001-timeplanning-gestion-absences` | **Date** : 2026-06-02 | **Spec** : [spec.md](./spec.md)
**Entrée** : Spécification de fonctionnalité depuis `/specs/001-timeplanning-gestion-absences/spec.md`

---

## Résumé

TimePlanning est une application interne web de gestion des absences permettant à des salariés de déposer des demandes d'absence, à des managers de les valider (workflow N+1 ou N+1+N+2), à la RH de piloter la clôture mensuelle et l'export paie, et à un admin technique de paramétrer l'application.

L'implémentation suit une architecture **DDD hexagonale** sur un backend Java 25 / Spring Boot 4.x organisé en Maven multi-modules, avec un frontend Angular / PrimeNG authentifié via Google OAuth2 / OIDC. L'ensemble est couvert par TDD avec JUnit 5 + JGiven + Allure.

---

## Contexte technique

**Langage/Version** : Java 25 (backend) · TypeScript / Angular latest (frontend)
**Framework backend** : Spring Boot 4.x — Spring Web MVC, Spring Security OAuth2 Resource Server, Spring Data JPA + Hibernate
**Validation** : Jakarta Bean Validation (Hibernate Validator)
**Migrations BD** : Liquibase (changesets versionnés et réversibles)
**ORM** : Hibernate 6.x via Spring Data JPA
**Dépendances principales** :
  - Backend : spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-oauth2-resource-server, spring-boot-starter-validation, liquibase-core, mariadb-java-client, poi (Excel export), jgiven-spring, allure-junit5
  - Frontend : @angular/*, primeng, @angular/material (optionnel), oidc-client-ts (keycloak-js écarté — pas de Keycloak dans la stack)

**Stockage** : MariaDB latest (mariadb4j embarqué en dev/test, serveur dédié en staging/prod)
**Tests** : JUnit 5, JGiven (ATDD/BDD), Allure (reporting), MockMvc (contrôleurs), Testcontainers (intégration MariaDB)
**Authentification** : Google OAuth2 (Authorization Server) → JWT Bearer token → Spring Security Resource Server
**Plateforme cible** : Serveur Linux (JVM), navigateur desktop + mobile (responsive)
**Type de projet** : Application web full-stack (API REST + SPA Angular)
**Objectifs de performance** : Réponse API p95 < 200 ms pour les opérations courantes ; export Excel < 30 s quelle que soit la volumétrie annuelle (CS-005)
**Contraintes** :
  - Aucun secret dans le dépôt ; `.env.example` maintenu à jour
  - Migrations Liquibase réversibles et testées en isolation
  - Complexité cyclomatique ≤ 10 par méthode (Checkstyle)
  - Couverture de tests non rédressable (CI gate)
**Échelle/Périmètre** : Application interne — quelques centaines d'utilisateurs, volumétrie faible (< 10 000 demandes/an)

---

## Vérification de la constitution

*PORTE : Validée avant la Phase 0 de recherche. À revérifier après la Phase 1 de conception.*

| Principe | Applicabilité | Mesure concrète | Statut |
|----------|---------------|-----------------|--------|
| **I. Qualité de code** | ✅ Tous les modules | Checkstyle (cyclomatique ≤ 10) ; Google Java Format ; ESLint + Prettier côté frontend ; revue obligatoire avant merge | ✅ OK |
| **II. Sécurité par défaut** | ✅ Tous les endpoints | Spring Security Resource Server valide le JWT à chaque requête ; Jakarta Bean Validation sur toutes les entrées ; OWASP Top 10 checklist par endpoint (IDOR, injection, CSRF) ; pas de secret dans le repo | ✅ OK |
| **III. TDD obligatoire** | ✅ Toutes les features | Red-Green-Refactor avec JUnit 5 ; scénarios JGiven mappés aux acceptance criteria de la spec ; MockMvc pour les contrôleurs ; Testcontainers pour l'intégration JPA/Liquibase | ✅ OK |
| **IV. Séparation des permissions** | ✅ Critical — 4 rôles distincts | `AuthorizationService` dédié dans le module `application` ; annotations `@PreAuthorize` déclaratives ; guards Angular par rôle ; tout accès protégé journalisé | ✅ OK |
| **V. Traçabilité métier** | ✅ Toutes les mutations | `EvenementAudit` persisté pour chaque action métier ; logs JSON (Logback + logstash-logback-encoder) avec `correlation_id` propagé via MDC ; rétention ≥ 90 jours | ✅ OK |
| **VI. UX simple et lisible** | ✅ SPA Angular/PrimeNG | Terminologie spec = terminologie UI = terminologie API ; feedback ≤ 200 ms (spinner si dépassé) ; WCAG 2.1 AA (PrimeNG respecte partiellement, audit manuel requis) ; mobile-first | ✅ OK |

**Dérogations** : aucune violation identifiée. Aucun suivi de complexité requis.

---

## Structure du projet

### Documentation (cette fonctionnalité)

```text
specs/001-timeplanning-gestion-absences/
├── plan.md              # Ce fichier
├── research.md          # Phase 0 — décisions techniques résolues
├── data-model.md        # Phase 1 — modèle de données DDD
├── quickstart.md        # Phase 1 — guide démarrage développeur
├── contracts/
│   ├── openapi.yaml     # Contrats REST API v1
│   └── notifications.md # Contrat des notifications email
└── tasks.md             # Phase 2 — généré par /speckit-tasks (NON créé ici)
```

### Code source (racine du dépôt)

Architecture **Maven multi-modules** suivant le modèle hexagonal (Ports & Adapters) :

```text
timeManager/                          # Racine du dépôt / module parent Maven
├── pom.xml                           # BOM parent, gestion des versions
│
├── domain/                           # Module : cœur métier (aucune dépendance externe)
│   └── src/main/java/fr/timemanager/domain/
│       ├── absence/                  # Agrégat Absence + VersionAbsence
│       ├── user/                     # Entité Utilisateur, rôles, hiérarchie
│       ├── team/                     # Entité Équipe, config workflow
│       ├── absencetype/              # TypeAbsence (référentiel)
│       ├── audit/                    # EvenementAudit (immuable)
│       ├── payroll/                  # PeriodeCloture, Regularisation
│       ├── recurrence/               # RecurrencePattern
│       ├── counter/                  # CompteurAbsence (agrégat calculé)
│       └── shared/                   # Value objects partagés (DateRange, HalfDay…)
│
├── application/                      # Module : cas d'usage, ports, DTOs
│   └── src/main/java/fr/timemanager/application/
│       ├── absence/                  # UseCases : CreateAbsenceRequest, ValidateAbsence…
│       ├── planning/                 # UseCase : GetMonthlyPlanning
│       ├── payroll/                  # UseCases : CloseMonth, GeneratePayrollExport
│       ├── recurrence/               # UseCase : InjectRecurrencePattern
│       ├── admin/                    # UseCases : ManageUsers, ManageTeams, ManageAbsenceTypes
│       ├── notification/             # Port (interface) NotificationService
│       ├── authorization/            # AuthorizationService — TOUTE la logique de permission
│       └── dto/                      # Requêtes et réponses applicatives
│
├── infrastructure/                   # Module : adaptateurs techniques
│   └── src/main/java/fr/timemanager/infrastructure/
│       ├── persistence/              # Repositories JPA, entités @Entity, Liquibase migrations
│       │   └── migration/            # db/changelog/ (changesets Liquibase)
│       ├── security/                 # JwtAuthenticationConverter, Google OAuth2 config
│       ├── notification/             # EmailNotificationService (JavaMailSender)
│       ├── export/                   # ExcelExportAdapter (Apache POI)
│       └── recurrence/               # RecurrenceGenerator (périodique)
│
├── api/                              # Module : couche HTTP (Spring MVC)
│   └── src/main/java/fr/timemanager/api/
│       ├── absence/                  # AbsenceController, validators
│       ├── planning/                 # PlanningController
│       ├── payroll/                  # PayrollController
│       ├── recurrence/               # RecurrenceController
│       ├── admin/                    # AdminController
│       ├── security/                 # SecurityConfig, @PreAuthorize guards
│       └── openapi/                  # Configuration Springdoc OpenAPI
│
├── frontend/                         # Module : SPA Angular + PrimeNG
│   ├── src/app/
│   │   ├── core/                     # Auth (OIDC), intercepteurs HTTP, guards Angular
│   │   ├── shared/                   # Composants réutilisables, pipes, directives
│   │   ├── features/
│   │   │   ├── my-absences/          # US1 — Mes demandes (salarié)
│   │   │   ├── validation/           # US2+3 — File de validation manager
│   │   │   ├── planning/             # US2 — Planning mensuel équipe
│   │   │   ├── counters/             # US1 — Compteurs d'absence
│   │   │   ├── payroll/              # US5 — Clôture et export paie (RH)
│   │   │   ├── recurrence/           # US6 — Module récurrences
│   │   │   └── admin/                # US7 — Administration technique
│   │   └── layout/                   # Shell, navigation, header
│   └── src/environments/
│
└── docker/                           # docker-compose.yml (MariaDB dev), Dockerfile
```

**Décision de structure** : Architecture hexagonale à 4 modules Maven (`domain`, `application`, `infrastructure`, `api`) + module `frontend`. Le module `domain` n'a aucune dépendance Spring — il est pur Java. L'`application` dépend uniquement de `domain`. L'`infrastructure` et l'`api` dépendent de l'`application`. Cette séparation garantit la testabilité du domaine sans infrastructure (principe III) et la centralisation de l'autorisation (principe IV).

---

## Suivi de complexité

> Aucune violation de la constitution identifiée — section non applicable pour cette fonctionnalité.
