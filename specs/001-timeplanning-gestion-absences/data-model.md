# Modèle de données — TimePlanning Gestion des Absences

**Phase 1 — Généré le** : 2026-06-02
**Architecture** : DDD hexagonal, Maven multi-modules

---

## Agrégats et entités du domaine

### Agrégat `Absence`

Racine d'agrégat principale. Regroupe toutes les versions d'une demande d'absence.

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `employeeId` | `UUID` | `employee_id` UUID FK → `users` | NOT NULL |
| `absenceTypeId` | `UUID` | `absence_type_id` UUID FK → `absence_types` | NOT NULL |
| `teamId` | `UUID` | `team_id` UUID FK → `teams` | NOT NULL (au moment de la demande) |
| `currentVersionId` | `UUID` | `current_version_id` UUID FK → `absence_versions` | nullable (avant première version) |
| `simplifiedStatus` | `AbsenceSimplifiedStatus` | `simplified_status` VARCHAR(20) | EN_ATTENTE, VALIDEE, REFUSEE, ANNULEE |
| `createdAt` | `Instant` | `created_at` TIMESTAMP | NOT NULL |
| `updatedAt` | `Instant` | `updated_at` TIMESTAMP | NOT NULL |
| `version` | `Long` | `version` BIGINT | Verrou optimiste Hibernate |

**Transitions de `simplifiedStatus`** : calculé depuis la version active courante.

---

### Entité `VersionAbsence`

Une version spécifique d'une demande. Plusieurs versions peuvent coexister ; une seule est `active`.

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `absenceId` | `UUID` | `absence_id` UUID FK → `absences` | NOT NULL |
| `versionNumber` | `int` | `version_number` INT | NOT NULL, >= 1 |
| `startDate` | `LocalDate` | `start_date` DATE | NOT NULL |
| `endDate` | `LocalDate` | `end_date` DATE | NOT NULL, >= start_date |
| `halfDayStart` | `HalfDayType` | `half_day_start` VARCHAR(10) | NONE, AM, PM — nullable |
| `halfDayEnd` | `HalfDayType` | `half_day_end` VARCHAR(10) | NONE, AM, PM — nullable |
| `workingDays` | `BigDecimal` | `working_days` DECIMAL(5,1) | NOT NULL, > 0 |
| `status` | `AbsenceVersionStatus` | `status` VARCHAR(30) | EN_ATTENTE, VALIDEE_N1, VALIDEE, REFUSEE, ANNULEE |
| `refusalReason` | `String` | `refusal_reason` TEXT | nullable, obligatoire si REFUSEE |
| `cancellationReason` | `String` | `cancellation_reason` TEXT | nullable, obligatoire si ANNULEE par manager |
| `submittedBy` | `UUID` | `submitted_by` UUID FK → `users` | NOT NULL (salarié ou manager délégué) |
| `active` | `boolean` | `active` BOOLEAN | Seule une version active par absence |
| `periodClosureId` | `UUID` | `period_closure_id` UUID FK → `period_closures` | nullable |
| `isRegularisation` | `boolean` | `is_regularisation` BOOLEAN | DEFAULT false |
| `createdAt` | `Instant` | `created_at` TIMESTAMP | NOT NULL |
| `version` | `Long` | `version` BIGINT | Verrou optimiste Hibernate |

**États possibles de `AbsenceVersionStatus`** :
```
EN_ATTENTE
  ├─→ VALIDEE_N1    (workflow double, N+1 valide)
  │     └─→ VALIDEE (N+2 valide)
  ├─→ VALIDEE       (workflow simple, ou N+2 court-circuite)
  └─→ REFUSEE       (N+1 ou N+2 refuse — terminal)
VALIDEE → ANNULEE   (manager annule avec justification)
```

---

### Entité `EvenementAudit`

Trace immuable de chaque action. Jamais modifiée après création.

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `absenceId` | `UUID` | `absence_id` UUID FK → `absences` | NOT NULL |
| `absenceVersionId` | `UUID` | `absence_version_id` UUID FK → `absence_versions` | nullable |
| `actionType` | `AuditActionType` | `action_type` VARCHAR(40) | NOT NULL |
| `authorId` | `UUID` | `author_id` UUID FK → `users` | NOT NULL |
| `authorRole` | `String` | `author_role` VARCHAR(20) | NOT NULL (rôle au moment de l'action) |
| `valueBefore` | `String` | `value_before` TEXT | JSON nullable |
| `valueAfter` | `String` | `value_after` TEXT | JSON nullable |
| `correlationId` | `UUID` | `correlation_id` UUID | NOT NULL |
| `occurredAt` | `Instant` | `occurred_at` TIMESTAMP | NOT NULL |

**`AuditActionType` valeurs** : `CREATION`, `VALIDATION_N1`, `VALIDATION_N2`, `VALIDATION_DIRECTE`, `REFUS`, `ANNULATION_SALARIE`, `ANNULATION_MANAGER`, `MODIFICATION_DEMANDEE`, `CORRECTION_RH`, `CLOTURE_PERIODE`, `REGULARISATION_DETECTEE`.

---

### Entité `User`

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `googleSub` | `String` | `google_sub` VARCHAR(255) | NOT NULL, UNIQUE (identifiant Google) |
| `email` | `String` | `email` VARCHAR(255) | NOT NULL, UNIQUE |
| `firstName` | `String` | `first_name` VARCHAR(100) | NOT NULL |
| `lastName` | `String` | `last_name` VARCHAR(100) | NOT NULL |
| `active` | `boolean` | `active` BOOLEAN | DEFAULT true |
| `nonWorkingDaysPattern` | `String` | `non_working_days_pattern` VARCHAR(50) | ex. `MON,FRI` — jours non travaillés contractuels |
| `recurrenceEnabled` | `boolean` | `recurrence_enabled` BOOLEAN | DEFAULT false (module US6) |
| `createdAt` | `Instant` | `created_at` TIMESTAMP | NOT NULL |

---

### Table de jonction `UserRole`

Un utilisateur peut avoir plusieurs rôles.

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `userId` | `UUID` | `user_id` UUID FK → `users` | PK composite |
| `role` | `UserRole` | `role` VARCHAR(20) | PK composite — SALARIE, MANAGER, RH, ADMIN |

---

### Entité `Team`

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `name` | `String` | `name` VARCHAR(100) | NOT NULL |
| `parentTeamId` | `UUID` | `parent_team_id` UUID FK → `teams` | nullable (sous-équipe) |
| `validationMode` | `ValidationMode` | `validation_mode` VARCHAR(20) | N1_ONLY, N1_AND_N2 |
| `n1ManagerId` | `UUID` | `n1_manager_id` UUID FK → `users` | NOT NULL |
| `n2ManagerId` | `UUID` | `n2_manager_id` UUID FK → `users` | nullable |
| `active` | `boolean` | `active` BOOLEAN | DEFAULT true |

---

### Table de jonction `TeamMember`

| Champ | Colonne SQL | Contraintes |
|-------|-------------|-------------|
| `teamId` | `team_id` UUID FK → `teams` | PK composite |
| `userId` | `user_id` UUID FK → `users` | PK composite |

---

### Entité `AbsenceType`

Référentiel paramétrable des types d'absence.

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `code` | `String` | `code` VARCHAR(20) | NOT NULL, UNIQUE |
| `label` | `String` | `label` VARCHAR(100) | NOT NULL |
| `color` | `String` | `color` VARCHAR(7) | Hex color `#RRGGBB` |
| `requiresManagerValidation` | `boolean` | `requires_manager_validation` BOOLEAN | DEFAULT true |
| `requiresJustification` | `boolean` | `requires_justification` BOOLEAN | DEFAULT false |
| `allowedRoles` | `Set<UserRole>` | Table `absence_type_roles` | Rôles autorisés à saisir |
| `isQuantified` | `boolean` | `is_quantified` BOOLEAN | Inclus dans compteurs |
| `annualReset` | `boolean` | `annual_reset` BOOLEAN | DEFAULT true |
| `allowsOverlap` | `boolean` | `allows_overlap` BOOLEAN | DEFAULT false (true pour type Ecole) |
| `active` | `boolean` | `active` BOOLEAN | DEFAULT true |

---

### Entité `PeriodeCloture`

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `year` | `int` | `year` INT | NOT NULL |
| `month` | `int` | `month` INT | NOT NULL, 1–12 |
| `closedAt` | `Instant` | `closed_at` TIMESTAMP | nullable (null = ouvert) |
| `closedByUserId` | `UUID` | `closed_by_user_id` UUID FK → `users` | nullable |
| `status` | `PeriodStatus` | `status` VARCHAR(10) | OPEN, CLOSED |

Contrainte UNIQUE : `(year, month)`.

---

### Entité `RecurrencePattern`

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `employeeId` | `UUID` | `employee_id` UUID FK → `users` | NOT NULL |
| `absenceTypeId` | `UUID` | `absence_type_id` UUID FK → `absence_types` | NOT NULL |
| `description` | `String` | `description` TEXT | nullable |
| `active` | `boolean` | `active` BOOLEAN | DEFAULT true |
| `createdAt` | `Instant` | `created_at` TIMESTAMP | NOT NULL |

---

### Entité `RecurrenceInjectionLog`

| Champ | Type Java | Colonne SQL | Contraintes |
|-------|-----------|-------------|-------------|
| `id` | `UUID` | `id` UUID PK | NOT NULL |
| `patternId` | `UUID` | `pattern_id` UUID FK → `recurrence_patterns` | NOT NULL |
| `source` | `InjectionSource` | `source` VARCHAR(20) | MANUAL, IMPORT, GENERATED |
| `injectedAt` | `Instant` | `injected_at` TIMESTAMP | NOT NULL |
| `injectedByUserId` | `UUID` | `injected_by_user_id` UUID FK → `users` | NOT NULL |
| `eventsCreatedCount` | `int` | `events_created_count` INT | NOT NULL |
| `periodStart` | `LocalDate` | `period_start` DATE | NOT NULL |
| `periodEnd` | `LocalDate` | `period_end` DATE | NOT NULL |

---

### Agrégat calculé `CompteurAbsence`

Non persisté directement — calculé à la demande depuis `absence_versions`.

```java
public record CompteurAbsence(
    UUID employeeId,
    UUID absenceTypeId,
    BigDecimal daysTakenPast,       // Absences validées dont endDate < aujourd'hui
    BigDecimal daysValidatedFuture, // Absences validées dont startDate >= aujourd'hui
    BigDecimal daysPending          // Absences EN_ATTENTE ou VALIDEE_N1
) {}
```

---

### Table de référence `PublicHoliday`

| Champ | Colonne SQL | Contraintes |
|-------|-------------|-------------|
| `date` | `holiday_date` DATE PK | NOT NULL |
| `label` | `label` VARCHAR(100) | NOT NULL |
| `year` | `year` INT | Index |

---

## Règles de validation métier (Jakarta Bean Validation + domaine)

| Règle | Localisation | Exigence |
|-------|--------------|----------|
| `endDate >= startDate` | `@AssertTrue` sur `VersionAbsence` | EF-001 |
| `workingDays > 0` | `@Positive` sur `workingDays` | EF-002 |
| Chevauchement absent (sauf Ecole) | Service domaine `AbsenceOverlapChecker` | EF-004, EF-005 |
| Refus avec motif obligatoire | `@NotBlank` conditionnel (status=REFUSEE) | EF-011 |
| Auto-validation interdite | `AuthorizationService.assertCannotSelfApprove()` | EF-010 |
| Salarié sans N+1 bloqué | `WorkflowEngine.assertManagerExists()` | Hypothèse spec |
| Type d'absence actif requis | `@AssertTrue` sur `AbsenceType.active` | EF-034 |

---

## Diagramme de relations (simplifié)

```
users ──── user_roles
  │
  ├── team_members ──── teams (N+1, N+2)
  │
  └── absences ──────── absence_versions ─── audit_events
        │                      │
        │                      └── period_closures
        │
        └── absence_types ─── absence_type_roles

recurrence_patterns ─── recurrence_injection_logs
public_holidays
```

---

## Transitions d'état complètes

```
AbsenceVersionStatus :

  [EN_ATTENTE]
      │ N+1 valide (workflow N1_ONLY)       → [VALIDEE]
      │ N+2 valide directement              → [VALIDEE]
      │ N+1 valide (workflow N1_AND_N2)     → [VALIDEE_N1]
      │ N+1 ou N+2 refuse                  → [REFUSEE] ──── terminal
      │ Salarié annule                      → [ANNULEE] ──── terminal
      │
  [VALIDEE_N1]
      │ N+2 valide                          → [VALIDEE]
      │ N+1 refuse (rétroactif)             → [REFUSEE] ──── terminal
      │
  [VALIDEE]
      │ Manager annule (+ justification)    → [ANNULEE] ──── terminal
      │ RH corrige                          → mutation EvenementAudit (statut inchangé)

AbsenceSimplifiedStatus (vue salarié) :
  VALIDEE_N1, EN_ATTENTE → EN_ATTENTE
  VALIDEE → VALIDEE
  REFUSEE → REFUSEE
  ANNULEE → ANNULEE
```
