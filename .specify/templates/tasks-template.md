---

description: "Template de liste de tâches pour l'implémentation d'une fonctionnalité"
---

# Tâches : [NOM DE LA FONCTIONNALITÉ]

**Entrée** : Documents de conception depuis `/specs/[###-nom-fonctionnalite]/`
**Prérequis** : plan.md (obligatoire), spec.md (obligatoire pour les user stories), research.md, data-model.md, contracts/

**Tests** : Les exemples ci-dessous incluent des tâches de test. Les tests sont OPTIONNELS — les inclure uniquement s'ils sont explicitement demandés dans la spécification.

**Organisation** : Les tâches sont regroupées par user story pour permettre une implémentation et des tests indépendants de chaque story.

## Format : `[ID] [P?] [Story] Description`

- **[P]** : Peut s'exécuter en parallèle (fichiers différents, pas de dépendances)
- **[Story]** : User story à laquelle cette tâche appartient (ex. US1, US2, US3)
- Inclure les chemins de fichiers exacts dans les descriptions

## Conventions de chemins

- **Projet unique** : `src/`, `tests/` à la racine du dépôt
- **Application web** : `backend/src/`, `frontend/src/`
- **Mobile** : `api/src/`, `ios/src/` ou `android/src/`
- Les chemins ci-dessous supposent un projet unique — à ajuster selon la structure dans plan.md

<!--
  ============================================================================
  IMPORTANT : Les tâches ci-dessous sont des TÂCHES EXEMPLES à titre illustratif.

  La commande /speckit.tasks DOIT les remplacer par les vraies tâches basées sur :
  - Les user stories de spec.md (avec leurs priorités P1, P2, P3...)
  - Les exigences fonctionnelles de plan.md
  - Les entités de data-model.md
  - Les endpoints de contracts/

  Les tâches DOIVENT être organisées par user story afin que chaque story puisse être :
  - Implémentée indépendamment
  - Testée indépendamment
  - Livrée comme un incrément MVP

  NE PAS conserver ces tâches exemples dans le fichier tasks.md généré.
  ============================================================================
-->

## Phase 1 : Initialisation (Infrastructure partagée)

**Objectif** : Initialisation du projet et structure de base

- [ ] T001 Créer la structure du projet selon le plan d'implémentation
- [ ] T002 Initialiser le projet [langage] avec les dépendances [framework]
- [ ] T003 [P] Configurer les outils de lint et de formatage

---

## Phase 2 : Fondations (Prérequis bloquants)

**Objectif** : Infrastructure centrale qui DOIT être complète avant toute user story

**⚠️ CRITIQUE** : Aucune user story ne peut démarrer tant que cette phase n'est pas terminée

Exemples de tâches fondationnelles (à adapter selon le projet) :

- [ ] T004 Mettre en place le schéma de base de données et le framework de migrations
- [ ] T005 [P] Implémenter le framework d'authentification/autorisation
- [ ] T006 [P] Mettre en place le routage API et la structure des middlewares
- [ ] T007 Créer les modèles/entités de base dont dépendent toutes les stories
- [ ] T008 Configurer la gestion des erreurs et l'infrastructure de journalisation
- [ ] T009 Mettre en place la gestion de la configuration d'environnement

**Point de contrôle** : Fondations prêtes — l'implémentation des user stories peut désormais débuter en parallèle

---

## Phase 3 : User Story 1 — [Titre] (Priorité : P1) 🎯 MVP

**Objectif** : [Description courte de ce que cette story apporte]

**Test indépendant** : [Comment vérifier que cette story fonctionne seule]

### Tests pour la User Story 1 (OPTIONNEL — uniquement si tests demandés) ⚠️

> **NOTE : Écrire ces tests EN PREMIER et s'assurer qu'ils ÉCHOUENT avant l'implémentation**

- [ ] T010 [P] [US1] Test de contrat pour [endpoint] dans tests/contrat/test_[nom].py
- [ ] T011 [P] [US1] Test d'intégration pour [parcours utilisateur] dans tests/integration/test_[nom].py

### Implémentation de la User Story 1

- [ ] T012 [P] [US1] Créer le modèle [Entité1] dans src/models/[entite1].py
- [ ] T013 [P] [US1] Créer le modèle [Entité2] dans src/models/[entite2].py
- [ ] T014 [US1] Implémenter [Service] dans src/services/[service].py (dépend de T012, T013)
- [ ] T015 [US1] Implémenter [endpoint/fonctionnalité] dans src/[emplacement]/[fichier].py
- [ ] T016 [US1] Ajouter validation et gestion des erreurs
- [ ] T017 [US1] Ajouter la journalisation pour les opérations de la User Story 1

**Point de contrôle** : La User Story 1 doit être entièrement fonctionnelle et testable de façon indépendante

---

## Phase 4 : User Story 2 — [Titre] (Priorité : P2)

**Objectif** : [Description courte de ce que cette story apporte]

**Test indépendant** : [Comment vérifier que cette story fonctionne seule]

### Tests pour la User Story 2 (OPTIONNEL — uniquement si tests demandés) ⚠️

- [ ] T018 [P] [US2] Test de contrat pour [endpoint] dans tests/contrat/test_[nom].py
- [ ] T019 [P] [US2] Test d'intégration pour [parcours utilisateur] dans tests/integration/test_[nom].py

### Implémentation de la User Story 2

- [ ] T020 [P] [US2] Créer le modèle [Entité] dans src/models/[entite].py
- [ ] T021 [US2] Implémenter [Service] dans src/services/[service].py
- [ ] T022 [US2] Implémenter [endpoint/fonctionnalité] dans src/[emplacement]/[fichier].py
- [ ] T023 [US2] Intégrer avec les composants de la User Story 1 (si nécessaire)

**Point de contrôle** : Les User Stories 1 ET 2 doivent fonctionner indépendamment

---

## Phase 5 : User Story 3 — [Titre] (Priorité : P3)

**Objectif** : [Description courte de ce que cette story apporte]

**Test indépendant** : [Comment vérifier que cette story fonctionne seule]

### Tests pour la User Story 3 (OPTIONNEL — uniquement si tests demandés) ⚠️

- [ ] T024 [P] [US3] Test de contrat pour [endpoint] dans tests/contrat/test_[nom].py
- [ ] T025 [P] [US3] Test d'intégration pour [parcours utilisateur] dans tests/integration/test_[nom].py

### Implémentation de la User Story 3

- [ ] T026 [P] [US3] Créer le modèle [Entité] dans src/models/[entite].py
- [ ] T027 [US3] Implémenter [Service] dans src/services/[service].py
- [ ] T028 [US3] Implémenter [endpoint/fonctionnalité] dans src/[emplacement]/[fichier].py

**Point de contrôle** : Toutes les user stories doivent être indépendamment fonctionnelles

---

[Ajouter d'autres phases de user story si nécessaire, en suivant le même schéma]

---

## Phase N : Finalisation & préoccupations transversales

**Objectif** : Améliorations affectant plusieurs user stories

- [ ] TXXX [P] Mise à jour de la documentation dans docs/
- [ ] TXXX Nettoyage du code et refactorisation
- [ ] TXXX Optimisation des performances sur toutes les stories
- [ ] TXXX [P] Tests unitaires supplémentaires (si demandés) dans tests/unitaire/
- [ ] TXXX Renforcement de la sécurité
- [ ] TXXX Journalisation des événements d'audit (traçabilité métier)
- [ ] TXXX Validation du quickstart.md

---

## Dépendances & ordre d'exécution

### Dépendances entre phases

- **Initialisation (Phase 1)** : Aucune dépendance — peut démarrer immédiatement
- **Fondations (Phase 2)** : Dépend de la Phase 1 — BLOQUE toutes les user stories
- **User Stories (Phase 3+)** : Toutes dépendent de la Phase 2
  - Les user stories peuvent ensuite avancer en parallèle (si l'équipe le permet)
  - Ou séquentiellement par ordre de priorité (P1 → P2 → P3)
- **Finalisation (Phase N)** : Dépend de la complétion de toutes les user stories souhaitées

### Dépendances entre user stories

- **User Story 1 (P1)** : Peut démarrer après la Phase 2 — aucune dépendance sur les autres stories
- **User Story 2 (P2)** : Peut démarrer après la Phase 2 — peut s'intégrer à US1 mais doit rester testable seule
- **User Story 3 (P3)** : Peut démarrer après la Phase 2 — peut s'intégrer à US1/US2 mais doit rester testable seule

### Au sein de chaque user story

- Les tests (si inclus) DOIVENT être écrits et ÉCHOUER avant l'implémentation
- Modèles avant services
- Services avant endpoints
- Implémentation principale avant intégration
- Story complète avant de passer à la priorité suivante

### Opportunités de parallélisation

- Toutes les tâches d'Initialisation marquées [P] peuvent s'exécuter en parallèle
- Toutes les tâches de Fondation marquées [P] peuvent s'exécuter en parallèle (au sein de la Phase 2)
- Une fois la Phase 2 terminée, toutes les user stories peuvent démarrer en parallèle (selon la capacité de l'équipe)
- Tous les tests d'une user story marqués [P] peuvent s'exécuter en parallèle
- Les modèles d'une même story marqués [P] peuvent s'exécuter en parallèle
- Différentes user stories peuvent être traitées en parallèle par différents membres de l'équipe

---

## Exemple de parallélisation : User Story 1

```bash
# Lancer tous les tests de la User Story 1 ensemble (si tests demandés) :
Tâche : "Test de contrat pour [endpoint] dans tests/contrat/test_[nom].py"
Tâche : "Test d'intégration pour [parcours utilisateur] dans tests/integration/test_[nom].py"

# Lancer tous les modèles de la User Story 1 ensemble :
Tâche : "Créer le modèle [Entité1] dans src/models/[entite1].py"
Tâche : "Créer le modèle [Entité2] dans src/models/[entite2].py"
```

---

## Stratégie d'implémentation

### MVP d'abord (User Story 1 uniquement)

1. Compléter la Phase 1 : Initialisation
2. Compléter la Phase 2 : Fondations (CRITIQUE — bloque toutes les stories)
3. Compléter la Phase 3 : User Story 1
4. **PAUSE ET VALIDATION** : Tester la User Story 1 de façon indépendante
5. Déployer/démo si prêt

### Livraison incrémentale

1. Initialisation + Fondations → Fondations prêtes
2. Ajouter User Story 1 → Tester seule → Déployer/Démo (MVP !)
3. Ajouter User Story 2 → Tester seule → Déployer/Démo
4. Ajouter User Story 3 → Tester seule → Déployer/Démo
5. Chaque story ajoute de la valeur sans casser les stories précédentes

### Stratégie équipe en parallèle

Avec plusieurs développeurs :

1. L'équipe complète l'Initialisation + Fondations ensemble
2. Une fois les Fondations terminées :
   - Développeur A : User Story 1
   - Développeur B : User Story 2
   - Développeur C : User Story 3
3. Les stories se terminent et s'intègrent indépendamment

---

## Notes

- Les tâches [P] = fichiers différents, pas de dépendances
- Le label [Story] relie la tâche à une user story précise pour la traçabilité
- Chaque user story doit être indépendamment réalisable et testable
- Vérifier que les tests échouent avant d'implémenter
- Committer après chaque tâche ou groupe logique
- S'arrêter à chaque point de contrôle pour valider la story de façon indépendante
- Éviter : tâches vagues, conflits sur un même fichier, dépendances inter-stories qui brisent l'indépendance
