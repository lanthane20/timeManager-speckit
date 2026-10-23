# Validation UX — protocole de test utilisateur

**Tâche** : T148 · **Statut** : ⛔ **en attente de session** — les résultats exigent des participants réels
**Critères couverts** : CS-001, CS-003, CS-008, CS-009

---

## Pourquoi ce document est incomplet

Les quatre critères ci-dessous mesurent des temps et une compréhension **humaine**. Ils ne peuvent pas
être établis par relecture du code ni par un test automatisé : ils supposent 3 managers et 1 admin
technique représentatifs, découvrant l'application sans formation préalable.

Ce document fournit le protocole prêt à dérouler et les tableaux à remplir pendant la session. Les
colonnes de résultat restent vides tant que la session n'a pas eu lieu.

---

## Participants requis

| Profil | Nombre | Critère justifiant leur présence |
|--------|--------|----------------------------------|
| Manager (encadre au moins un salarié) | 3 | CS-003, CS-009 |
| Admin technique | 1 | CS-008 |
| Salarié (peut être l'un des managers, sur son propre compte) | 1 | CS-001 |

**Condition impérative** : aucun participant ne doit avoir reçu de formation ni de démonstration
préalable — les critères portent explicitement sur la prise en main sans accompagnement.

---

## Préparation de l'environnement

1. Déployer l'application sur l'environnement de recette (voir [`quickstart.md`](./quickstart.md)).
2. Créer les comptes des participants via le module d'administration et les rattacher à une équipe
   disposant d'un N+1.
3. Alimenter le mois courant avec une dizaine d'absences de types et de statuts variés, réparties sur
   au moins 4 salariés — sans quoi le planning de CS-009 serait vide et non représentatif.
4. Vérifier que les notifications email sont actives (`NOTIFICATION_ENABLED=true`) : CS-003 se mesure
   à partir de la réception de la notification.

---

## Protocole par critère

### CS-001 — Création d'une demande en moins de 2 minutes, sans formation

**Consigne au participant** : « Vous partez en congés du 15 au 19 juin. Déclarez-le dans
l'application. »

**Mesure** : chronomètre déclenché à l'affichage de l'écran d'accueil, arrêté à la confirmation
d'enregistrement. Ne pas intervenir, même en cas d'hésitation ; noter les blocages.

| Participant | Temps mesuré | Hésitations observées | Objectif < 2 min |
|-------------|--------------|-----------------------|------------------|
| P1 | | | ☐ |
| P2 | | | ☐ |
| P3 | | | ☐ |

### CS-003 — Traitement d'une demande en moins d'une minute depuis la notification

**Consigne** : « Vous venez de recevoir cet email. Traitez la demande. » (moitié des participants :
valider ; autre moitié : refuser, afin d'éprouver aussi la saisie du motif obligatoire.)

**Mesure** : du clic sur le lien de l'email à la confirmation de la décision.

| Participant | Action | Temps mesuré | Objectif < 1 min |
|-------------|--------|--------------|------------------|
| P1 | | | ☐ |
| P2 | | | ☐ |
| P3 | | | ☐ |

### CS-008 — Création d'un type d'absence en moins de 5 minutes

**Consigne à l'admin** : « Créez un type d'absence "Congé sans solde", validé par le manager, non
comptabilisé dans les compteurs, et rendez-le disponible à la saisie. »

**Mesure** : de l'ouverture du module d'administration à la vérification que le type apparaît dans le
formulaire de demande d'un salarié.

| Participant | Temps mesuré | Objectif < 5 min |
|-------------|--------------|------------------|
| Admin | | ☐ |

### CS-009 — Planning mensuel compris au premier coup d'œil par 90 % des managers

**Consigne** : afficher le planning mensuel de l'équipe **sans aucune explication**, puis demander
immédiatement : « Qui est absent le 12 ? Quelqu'un a-t-il une demande encore en attente ? »

**Mesure** : réponse correcte aux deux questions, sans aide et sans survol prolongé.

| Participant | Absents identifiés | Demandes en attente identifiées | Compris au premier coup d'œil |
|-------------|--------------------|---------------------------------|-------------------------------|
| P1 | | | ☐ |
| P2 | | | ☐ |
| P3 | | | ☐ |

**Seuil de réussite** : 90 % des participants, soit les 3 managers sur un échantillon de 3.

---

## Synthèse (à compléter après la session)

| Critère | Objectif | Résultat mesuré | Statut |
|---------|----------|-----------------|--------|
| CS-001 | < 2 min | | ☐ atteint / ☐ non atteint |
| CS-003 | < 1 min | | ☐ atteint / ☐ non atteint |
| CS-008 | < 5 min | | ☐ atteint / ☐ non atteint |
| CS-009 | 90 % de compréhension | | ☐ atteint / ☐ non atteint |

**Décision** : un critère non atteint ouvre une correction d'interface avant mise en production. Les
observations qualitatives (hésitations, vocabulaire mal compris) sont à consigner ci-dessous, même
lorsque le temps objectif est tenu — elles alimentent le Principe VI de la Constitution
(terminologie métier cohérente entre UI, API et documentation).

### Observations qualitatives

_À compléter pendant la session._
