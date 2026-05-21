# Plan d'implémentation : [FONCTIONNALITÉ]

**Branche** : `[###-nom-fonctionnalite]` | **Date** : [DATE] | **Spec** : [lien]
**Entrée** : Spécification de fonctionnalité depuis `/specs/[###-nom-fonctionnalite]/spec.md`

**Note** : Ce template est rempli par la commande `/speckit.plan`. Voir `.specify/templates/plan-template.md` pour le flux d'exécution.

## Résumé

[Extrait de la spec : exigence principale + approche technique issue de la recherche]

## Contexte technique

<!--
  ACTION REQUISE : Remplacer le contenu de cette section par les détails techniques
  du projet. La structure présentée ici est indicative pour guider le processus d'itération.
-->

**Langage/Version** : [ex. Python 3.11, Swift 5.9, Rust 1.75 ou À PRÉCISER]
**Dépendances principales** : [ex. FastAPI, UIKit, LLVM ou À PRÉCISER]
**Stockage** : [si applicable, ex. PostgreSQL, CoreData, fichiers ou N/A]
**Tests** : [ex. pytest, XCTest, cargo test ou À PRÉCISER]
**Plateforme cible** : [ex. Serveur Linux, iOS 15+, WASM ou À PRÉCISER]
**Type de projet** : [ex. bibliothèque/cli/service-web/app-mobile/compilateur/app-bureau ou À PRÉCISER]
**Objectifs de performance** : [spécifique au domaine, ex. 1000 req/s, 10k lignes/s, 60 fps ou À PRÉCISER]
**Contraintes** : [spécifique au domaine, ex. <200ms p95, <100Mo mémoire, hors-ligne ou À PRÉCISER]
**Échelle/Périmètre** : [spécifique au domaine, ex. 10k utilisateurs, 1M LOC, 50 écrans ou À PRÉCISER]

## Vérification de la constitution

*PORTE : Doit être validée avant la Phase 0 de recherche. À revérifier après la Phase 1 de conception.*

[Portes déterminées à partir du fichier de constitution]

## Structure du projet

### Documentation (cette fonctionnalité)

```text
specs/[###-fonctionnalite]/
├── plan.md              # Ce fichier (sortie de la commande /speckit.plan)
├── research.md          # Sortie Phase 0 (commande /speckit.plan)
├── data-model.md        # Sortie Phase 1 (commande /speckit.plan)
├── quickstart.md        # Sortie Phase 1 (commande /speckit.plan)
├── contracts/           # Sortie Phase 1 (commande /speckit.plan)
└── tasks.md             # Sortie Phase 2 (commande /speckit.tasks — NON créé par /speckit.plan)
```

### Code source (racine du dépôt)

<!--
  ACTION REQUISE : Remplacer l'arborescence ci-dessous par la structure concrète
  de cette fonctionnalité. Supprimer les options non retenues et développer la
  structure choisie avec les vrais chemins. Le plan livré ne doit pas conserver
  les étiquettes d'option.
-->

```text
# [SUPPRIMER SI INUTILISÉ] Option 1 : Projet unique (PAR DÉFAUT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contrat/
├── integration/
└── unitaire/

# [SUPPRIMER SI INUTILISÉ] Option 2 : Application web (quand "frontend" + "backend" détectés)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [SUPPRIMER SI INUTILISÉ] Option 3 : Mobile + API (quand "iOS/Android" détecté)
api/
└── [identique à backend ci-dessus]

ios/ ou android/
└── [structure spécifique à la plateforme : modules de fonctionnalité, flux UI, tests plateforme]
```

**Décision de structure** : [Documenter la structure retenue et référencer les répertoires réels ci-dessus]

## Suivi de complexité

> **À remplir UNIQUEMENT si la vérification de la constitution révèle des violations devant être justifiées**

| Violation | Pourquoi nécessaire | Alternative plus simple rejetée parce que |
|-----------|---------------------|-------------------------------------------|
| [ex. 4e projet] | [besoin actuel] | [pourquoi 3 projets insuffisants] |
| [ex. Repository pattern] | [problème spécifique] | [pourquoi accès BD direct insuffisant] |
