# CLAUDE.md

Ce fichier fournit des indications à Claude Code (claude.ai/code) pour travailler dans ce dépôt.

## Langue de communication

**Toutes les réponses et communications avec l'utilisateur doivent être en français.** Cela inclut les explications, les questions de clarification, les résumés de tâches et les messages d'erreur.

**Tous les fichiers Markdown générés doivent être rédigés en français.** Cela inclut les spécifications, plans, tâches, checklists et tout autre document produit dans le cadre du workflow.

## Vue d'ensemble du projet

TimePlanning est une application interne de gestion des absences et de planning d'équipe, actuellement en **phase de spécification**. Le dépôt utilise le framework **Specify (spec-kit) v0.5.1.dev0** pour piloter un workflow structuré spec → plan → implémentation. Il n'y a pas encore de code source applicatif.

- **Spécification produit** : [`specs/001-timeplanning-gestion-absences/spec.md`](specs/001-timeplanning-gestion-absences/spec.md) — 7 user stories (P1–P3), 38 exigences fonctionnelles, 4 rôles (Salarié, Manager, RH, Admin technique)
- **Constitution** (référence qualité) : [`.specify/memory/constitution.md`](.specify/memory/constitution.md)
- **Config framework** : [`.specify/init-options.json`](.specify/init-options.json) — numérotation séquentielle des branches, scripts PowerShell, intégration Claude AI

## Commandes du workflow Specify

Chaque étape du workflow s'invoque sous forme de skill Claude Code (taper `/nomduskill` dans le chat) :

| Commande | Phase | Rôle |
|----------|-------|------|
| `/speckit-specify` | 1 — Spécification | Générer une spec fonctionnelle à partir d'une description |
| `/speckit-analyze` | 1b — Analyse | Analyser et critiquer une spec existante |
| `/speckit-clarify` | 2 — Clarification | Résoudre les questions ouvertes avant la planification |
| `/speckit-plan` | 3 — Planification | Générer le plan d'implémentation depuis la spec |
| `/speckit-tasks` | 4 — Découpage | Décomposer le plan en tâches de développement |
| `/speckit-taskstoissues` | 4b — Issues | Pousser les tâches vers GitHub Issues |
| `/speckit-implement` | 5 — Implémentation | Exécuter une tâche (cycle TDD) |
| `/speckit-checklist` | Toute phase | Valider une spec ou un plan via la checklist qualité |
| `/speckit-constitution` | Toute phase | Consulter ou amender la constitution du projet |

La prochaine étape pour ce projet est `/speckit-plan` sur la spec existante.

## Structure du dépôt

```
specs/                            # Spécifications fonctionnelles
  001-timeplanning-gestion-absences/
    spec.md                       # Spec fonctionnelle (source de vérité produit)
    checklists/requirements.md    # Checklist qualité (tous les points validés)

.specify/
  memory/constitution.md          # Principes du projet v1.0.0 — régit toute l'implémentation
  templates/                      # spec-template.md, plan-template.md, tasks-template.md
  scripts/powershell/             # create-new-feature.ps1, setup-plan.ps1, etc.
  init-options.json               # branch_numbering: sequential, script: ps

.claude/skills/                   # Définitions des skills Specify chargés par Claude Code
```

## Principes de la Constitution (non négociables)

Les six principes de `.specify/memory/constitution.md` s'appliquent à tout le travail d'implémentation :

1. **Qualité de code** — Complexité cyclomatique ≤ 10 par fonction ; aucune abstraction non immédiatement nécessaire (YAGNI) ; lint et formatage doivent passer sans erreur avant tout commit.
2. **Sécurité par défaut** — Principe du moindre privilège ; validation de toute entrée externe ; risques OWASP Top 10 traités pour chaque endpoint ; aucun secret dans le dépôt.
3. **TDD obligatoire** — Red-Green-Refactor ; aucun PR ne peut réduire la couverture de tests ; tests de contrat requis pour toute interface publique.
4. **Séparation stricte des permissions** — Logique d'autorisation dans une couche dédiée uniquement (jamais inline dans les contrôleurs) ; guards distincts par rôle ; tout accès à une ressource protégée doit être journalisé.
5. **Traçabilité métier** — Logs JSON structurés avec `timestamp`, `correlation_id`, `user_id`, `action`, `resource`, `result` ; toute mutation métier émet un événement d'audit persisté.
6. **UX simple et lisible** — Terminologie métier cohérente entre UI, API et documentation ; retour visible ≤ 200 ms ; WCAG 2.1 AA ; conception mobile-first.

## Portes de validation avant merge

Chaque PR doit franchir ces portes avant fusion :
- CI au vert (lint + tests unitaires + tests d'intégration + audit sécurité)
- `plan.md` liste les principes applicables et documente toute dérogation
- Revue de sécurité pour tout nouvel endpoint ou règle de permission
- Test E2E ou capture d'écran pour tout changement d'interface
- Événements d'audit démontrés en revue pour toute nouvelle opération métier

## Conventions Git

- **Nommage des branches** : préfixe numérique séquentiel — `001-nom-feature`, `002-nom-feature` (attribué automatiquement par `create-new-feature.ps1`)
- **Pas de push direct sur main** — tous les changements passent par des PRs revues
- **ADR obligatoire** pour tout changement majeur de stack (langage, framework, base de données)

## Standards de développement (pour la phase d'implémentation)

- Les endpoints API doivent être versionnés : `/api/v1/...`
- Tout endpoint documenté en OpenAPI/Swagger avant mise en production
- Les migrations de base de données doivent être versionnées, réversibles et testées en isolation
- `.env.example` doit rester synchronisé avec toutes les variables requises (sans valeurs sensibles)
