# Contrat de notifications email — TimePlanning

**Phase 1 — Généré le** : 2026-06-02
**Exigences couvertes** : EF-036, EF-037

---

## Principe général

Chaque événement métier déclenche l'envoi d'un email via le port `NotificationService` (interface dans le module `application`). L'implémentation concrète réside dans `infrastructure/notification/EmailNotificationService`.

Les emails ne bloquent pas le flux métier — ils sont envoyés de façon asynchrone (Spring `@Async`).

---

## Événements et destinataires

| Événement | Déclencheur | Destinataire(s) |
|-----------|-------------|-----------------|
| `ABSENCE_CREATED` | Salarié soumet une demande | N+1 du salarié (EF-037 : uniquement N+1 en workflow double) |
| `ABSENCE_VALIDATED` | Manager (N+1 ou N+2) valide | Salarié demandeur |
| `ABSENCE_REFUSED` | Manager refuse | Salarié demandeur |
| `ABSENCE_CANCELLED_BY_EMPLOYEE` | Salarié annule | N+1 (information) |
| `ABSENCE_CANCELLED_BY_MANAGER` | Manager annule une absence validée | Salarié demandeur |
| `ABSENCE_MODIFICATION_REQUESTED` | Salarié demande modification | N+1 du salarié |
| `ABSENCE_N1_VALIDATED` | N+1 valide en workflow double | N+2 du salarié |

---

## Structure des notifications (interface)

```java
// Port dans le module application
public interface NotificationService {

    void notifyAbsenceCreated(AbsenceCreatedEvent event);
    void notifyAbsenceValidated(AbsenceValidatedEvent event);
    void notifyAbsenceRefused(AbsenceRefusedEvent event);
    void notifyAbsenceCancelledByEmployee(AbsenceCancelledEvent event);
    void notifyAbsenceCancelledByManager(AbsenceCancelledEvent event);
    void notifyModificationRequested(AbsenceModificationRequestedEvent event);
    void notifyN2PendingValidation(AbsenceN1ValidatedEvent event);
}
```

---

## Contenu minimal de chaque email

### `ABSENCE_CREATED` → N+1

- **Objet** : `[TimePlanning] Nouvelle demande d'absence — {Prénom Nom}`
- **Corps** :
  - Salarié : {Prénom Nom}
  - Type d'absence : {libellé}
  - Du : {date début} au {date fin} ({nb jours} jours ouvrés)
  - Lien direct vers la demande dans l'application

### `ABSENCE_VALIDATED` → Salarié

- **Objet** : `[TimePlanning] Votre demande d'absence a été validée`
- **Corps** :
  - Type et dates de l'absence
  - Validée par : {Prénom Nom manager}
  - Lien vers "Mes demandes"

### `ABSENCE_REFUSED` → Salarié

- **Objet** : `[TimePlanning] Votre demande d'absence a été refusée`
- **Corps** :
  - Type et dates de l'absence
  - Refusée par : {Prénom Nom manager}
  - Motif : {motif de refus — obligatoire, EF-011}
  - Lien vers "Mes demandes"

### `ABSENCE_N1_VALIDATED` → N+2 (workflow double uniquement)

- **Objet** : `[TimePlanning] Demande en attente de votre validation — {Prénom Nom salarié}`
- **Corps** :
  - Idem `ABSENCE_CREATED` avec mention que le N+1 a déjà validé
  - Lien direct vers la demande

---

## Contraintes

- Les emails ne contiennent **pas de données d'absence brutes exportables** (conformité RGPD : accès via l'application authentifiée).
- Les modèles d'email sont des templates Thymeleaf dans `infrastructure/src/main/resources/templates/email/`.
- En profil `dev`, les emails sont redirigés vers la console ou un serveur SMTP local (ex. MailHog) via `spring.mail.host=localhost`.
- La désactivation globale des notifications se fait via la variable `NOTIFICATION_ENABLED=false` (utile en tests et dev).

---

## Traçabilité

Chaque envoi d'email est journalisé dans les logs structurés :

```json
{
  "timestamp": "...",
  "correlation_id": "...",
  "user_id": "destinataire-sub-pseudonymisé",
  "action": "EMAIL_SENT",
  "resource": "absence/{id}",
  "result": "SUCCESS | FAILURE",
  "emailType": "ABSENCE_CREATED"
}
```

Les échecs d'envoi sont logués en `ERROR` mais ne font **pas** échouer la transaction métier principale.
