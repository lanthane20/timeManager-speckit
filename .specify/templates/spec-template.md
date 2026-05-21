# Spécification de fonctionnalité : [NOM DE LA FONCTIONNALITÉ]

**Branche** : `[###-nom-fonctionnalite]`
**Créée** : [DATE]
**Statut** : Brouillon
**Entrée** : Description utilisateur : "$ARGUMENTS"

## Scénarios utilisateur & tests *(obligatoires)*

<!--
  IMPORTANT : Les user stories doivent être PRIORISÉES en tant que parcours utilisateur
  ordonnés par importance. Chaque user story/parcours doit être TESTABLE DE FAÇON INDÉPENDANTE,
  c'est-à-dire que si on n'implémente qu'UNE SEULE d'entre elles, on doit obtenir un MVP
  (Produit Minimum Viable) qui apporte de la valeur.

  Attribuer des priorités (P1, P2, P3, etc.) à chaque story, P1 étant la plus critique.
  Chaque story est une tranche de fonctionnalité autonome pouvant être :
  - Développée indépendamment
  - Testée indépendamment
  - Déployée indépendamment
  - Démontrée aux utilisateurs indépendamment
-->

### User Story 1 — [Titre court] (Priorité : P1)

[Décrire ce parcours utilisateur en langage naturel]

**Pourquoi cette priorité** : [Expliquer la valeur apportée et la raison de ce niveau de priorité]

**Test indépendant** : [Décrire comment cette story peut être testée seule — ex. "Peut être
entièrement testée en [action spécifique] et livre [valeur spécifique]"]

**Scénarios d'acceptation** :

1. **Étant donné** [état initial], **quand** [action], **alors** [résultat attendu]
2. **Étant donné** [état initial], **quand** [action], **alors** [résultat attendu]

---

### User Story 2 — [Titre court] (Priorité : P2)

[Décrire ce parcours utilisateur en langage naturel]

**Pourquoi cette priorité** : [Expliquer la valeur apportée et la raison de ce niveau de priorité]

**Test indépendant** : [Décrire comment cette story peut être testée seule]

**Scénarios d'acceptation** :

1. **Étant donné** [état initial], **quand** [action], **alors** [résultat attendu]

---

### User Story 3 — [Titre court] (Priorité : P3)

[Décrire ce parcours utilisateur en langage naturel]

**Pourquoi cette priorité** : [Expliquer la valeur apportée et la raison de ce niveau de priorité]

**Test indépendant** : [Décrire comment cette story peut être testée seule]

**Scénarios d'acceptation** :

1. **Étant donné** [état initial], **quand** [action], **alors** [résultat attendu]

---

[Ajouter d'autres user stories si nécessaire, chacune avec une priorité assignée]

### Cas limites

<!--
  ACTION REQUISE : Le contenu de cette section représente des exemples indicatifs.
  Les remplacer par les vrais cas limites applicables à la fonctionnalité.
-->

- Que se passe-t-il quand [condition limite] ?
- Comment le système gère-t-il [scénario d'erreur] ?

## Exigences *(obligatoires)*

<!--
  ACTION REQUISE : Le contenu de cette section représente des exemples indicatifs.
  Les remplacer par les vraies exigences fonctionnelles.
-->

### Exigences fonctionnelles

- **EF-001** : Le système DOIT [capacité spécifique, ex. "permettre aux utilisateurs de créer un compte"]
- **EF-002** : Le système DOIT [capacité spécifique, ex. "valider les adresses e-mail"]
- **EF-003** : Les utilisateurs DOIVENT pouvoir [interaction clé, ex. "réinitialiser leur mot de passe"]
- **EF-004** : Le système DOIT [exigence de données, ex. "persister les préférences utilisateur"]
- **EF-005** : Le système DOIT [comportement, ex. "journaliser tous les événements de sécurité"]

*Exemple de marquage d'exigences non claires :*

- **EF-006** : Le système DOIT authentifier les utilisateurs via [À PRÉCISER : méthode d'auth non spécifiée — email/mot de passe, SSO, OAuth ?]
- **EF-007** : Le système DOIT conserver les données utilisateur pendant [À PRÉCISER : durée de rétention non spécifiée]

### Entités clés *(inclure si la fonctionnalité implique des données)*

- **[Entité 1]** : [Ce qu'elle représente, attributs clés sans détails d'implémentation]
- **[Entité 2]** : [Ce qu'elle représente, relations avec les autres entités]

## Critères de succès *(obligatoires)*

<!--
  ACTION REQUISE : Définir des critères de succès mesurables.
  Ils doivent être indépendants de la technologie et vérifiables.
-->

### Résultats mesurables

- **CS-001** : [Métrique mesurable, ex. "Les utilisateurs peuvent finaliser la création de compte en moins de 2 minutes"]
- **CS-002** : [Métrique mesurable, ex. "Le système supporte 1000 utilisateurs simultanés sans dégradation"]
- **CS-003** : [Métrique de satisfaction, ex. "90% des utilisateurs réussissent la tâche principale dès le premier essai"]
- **CS-004** : [Métrique métier, ex. "Réduire de 50% les tickets de support liés à [X]"]

## Hypothèses

<!--
  ACTION REQUISE : Le contenu de cette section représente des exemples indicatifs.
  Les remplacer par les vraies hypothèses basées sur des valeurs par défaut raisonnables
  choisies lorsque la description ne précisait pas certains détails.
-->

- [Hypothèse sur les utilisateurs cibles, ex. "Les utilisateurs disposent d'une connexion internet stable"]
- [Hypothèse sur les limites de périmètre, ex. "Le support mobile est hors périmètre pour v1"]
- [Hypothèse sur les données/environnement, ex. "Le système d'authentification existant sera réutilisé"]
- [Dépendance vis-à-vis d'un système/service existant, ex. "Nécessite l'accès à l'API de profil utilisateur existante"]
