<!--
SYNC IMPACT REPORT
==================
Version change: (none) → 1.0.0 (ratification initiale)

Principes modifiés: N/A — première édition

Sections ajoutées:
  - Core Principles (6 principes)
  - Standards de développement
  - Processus de validation
  - Governance

Sections supprimées: N/A

Templates vérifiés:
  ✅ .specify/templates/plan-template.md
       — Constitution Check section présente ; les portes de qualité sont maintenant
         définies par les 6 principes ci-dessous (à utiliser lors de /speckit.plan).
  ✅ .specify/templates/spec-template.md
       — Sections obligatoires (User Stories, Requirements, Success Criteria) déjà
         présentes ; aucune modification structurelle requise.
  ✅ .specify/templates/tasks-template.md
       — Phase N (Polish) inclut déjà "Security hardening" et des tâches de
         documentation ; les principes Traçabilité et Permissions sont couverts par
         les catégories de tâches existantes.
  ℹ  .specify/templates/commands/ — Répertoire absent ; aucun fichier à mettre à jour.

TODOs différés: Aucun — tous les placeholders ont été résolus.
-->

# TimePlanning Constitution

## Principes fondamentaux

### I. Qualité de code

Tout code livré DOIT respecter les règles suivantes :

- **Lisibilité** : Noms de variables, fonctions et classes explicites ; aucune abréviation
  non documentée n'est autorisée.
- **Revue obligatoire** : Aucune fusion de branche ne DOIT avoir lieu sans revue approuvée
  par au moins un pair.
- **Complexité maîtrisée** : La complexité cyclomatique DOIT rester ≤ 10 par fonction ;
  toute exception DOIT être justifiée dans la description du PR.
- **YAGNI** : Aucune abstraction ou généralisation non immédiatement nécessaire ne DOIT
  être introduite.
- **Formatage automatique** : Les outils de lint et de formatage (ESLint, Prettier, Black,
  etc.) DOIVENT passer sans erreur avant tout commit.

*Raison :* Une base de code lisible et peu complexe réduit le coût de maintenance et
limite les régressions lors des évolutions fonctionnelles.

### II. Sécurité par défaut

La sécurité DOIT être intégrée dès la conception, jamais ajoutée en dernière étape :

- **Principe du moindre privilège** : Toute entité (utilisateur, service, token) ne
  DOIT disposer que des droits strictement nécessaires à son rôle.
- **Validation à la frontière** : Toute donnée externe (entrées utilisateur, réponses
  d'API tierces) DOIT être validée et assainie avant traitement.
- **Secrets hors du dépôt** : Clés API, mots de passe et tokens DOIVENT être stockés dans
  des variables d'environnement ou un coffre-fort ; ils ne DOIVENT JAMAIS apparaître dans
  le code source ni dans l'historique git.
- **OWASP Top 10** : Les risques OWASP (injection SQL, XSS, CSRF, IDOR, etc.) DOIVENT être
  explicitement considérés pour chaque endpoint ou composant exposé.
- **Dépendances auditées** : Toute dépendance tierce DOIT être auditée (`npm audit`,
  `pip-audit`, ou équivalent) à chaque ajout ou mise à jour majeure.

*Raison :* Une faille de sécurité impacte directement la confiance des utilisateurs,
la conformité légale (RGPD) et la réputation du produit.

### III. Tests obligatoires

Les tests sont NON NÉGOCIABLES. Le cycle TDD s'applique par défaut à toute nouvelle
fonctionnalité :

- **Red-Green-Refactor** : Les tests DOIVENT être écrits et échouer avant l'implémentation
  de la fonctionnalité.
- **Couverture minimale** : Toute fonctionnalité nouvelle DOIT être couverte par des tests
  unitaires et, si applicable, des tests d'intégration.
- **Tests de contrat** : Tout endpoint ou interface publique exposée à d'autres modules
  DOIT disposer d'un test de contrat.
- **Zéro régression** : Aucun PR ne DOIT réduire la couverture de tests existante.
- **CI obligatoire** : La suite de tests complète DOIT s'exécuter automatiquement dans le
  pipeline CI à chaque push sur une branche active.

*Raison :* Les tests constituent la première défense contre les régressions et documentent
le comportement attendu de manière exécutable.

### IV. Séparation stricte des permissions backend

Le contrôle d'accès DOIT être centralisé, explicite et jamais implicite :

- **Couche d'autorisation dédiée** : La logique de vérification des permissions DOIT
  résider dans une couche distincte (middleware ou service d'autorisation) ; elle ne DOIT
  PAS être dupliquée dans les contrôleurs ou les resolvers.
- **Rôles déclaratifs** : Les permissions DOIVENT être définies de façon déclarative
  (RBAC, ABAC ou policy-based) ; aucune logique de permission ad hoc inline n'est autorisée.
- **Surfaces d'API distinctes** : Les administrateurs, gestionnaires et employés DOIVENT
  disposer de guards explicites ou de routes séparées ; un rôle ne DOIT jamais pouvoir
  appeler un endpoint réservé à un rôle supérieur.
- **Audit des accès** : Tout accès à une ressource protégée DOIT être journalisé avec
  l'identité de l'appelant et le résultat (accordé / refusé).
- **Tests de permission** : Chaque rôle DOIT être couvert par des tests vérifiant
  explicitement ce à quoi il a accès et ce à quoi il n'a PAS accès.

*Raison :* Une gestion centralisée des permissions prévient les escalades de privilèges et
simplifie les audits de sécurité et de conformité.

### V. Traçabilité métier

Chaque opération métier significative DOIT être tracée de bout en bout :

- **Journalisation structurée** : Les logs DOIVENT être au format structuré (JSON) et
  inclure au minimum : `timestamp`, `correlation_id`, `user_id`, `action`, `resource`,
  `result`.
- **Auditabilité** : Les créations, modifications et suppressions de données métier
  (plannings, temps saisis, validations de congés) DOIVENT générer un événement d'audit
  persisté et horodaté.
- **Identifiants de corrélation** : Toute requête DOIT propager un `correlation_id` entre
  les couches (API → service → base de données).
- **Rétention** : Les logs d'audit DOIVENT être conservés selon la politique définie
  (minimum 90 jours sauf obligation légale supérieure).
- **Protection des données personnelles** : Les logs NE DOIVENT PAS contenir de données
  personnelles non pseudonymisées (RGPD).

*Raison :* La traçabilité est indispensable pour le support opérationnel, les audits RH
et légaux, et le débogage en production.

### VI. UX simple et lisible

L'interface utilisateur DOIT privilégier la clarté sur l'exhaustivité :

- **Progressive disclosure** : Seules les informations nécessaires au contexte courant DOIVENT
  être affichées ; les détails avancés sont accessibles à la demande.
- **Terminologie cohérente** : Les mêmes termes métier DOIVENT être utilisés dans l'UI,
  les API et la documentation (ex. "pointage", "congé validé", "planning hebdomadaire").
- **Feedback immédiat** : Toute action utilisateur DOIT produire un retour visible en moins
  de 200 ms ; un indicateur de progression DOIT apparaître si l'opération dure plus longtemps.
- **Accessibilité** : Le niveau WCAG 2.1 AA DOIT être respecté pour toutes les interfaces
  exposées aux utilisateurs finaux.
- **Mobile-first** : Les interfaces DOIVENT être conçues et testées sur mobile avant les
  écrans larges, compte tenu de l'usage terrain quotidien.

*Raison :* Une UX claire réduit les erreurs de saisie et le coût de formation, essentiel
pour un outil de gestion du temps utilisé chaque jour par des équipes non techniques.

## Standards de développement

Les contraintes techniques suivantes s'appliquent à l'ensemble du projet TimePlanning :

- **Décisions architecturales** : Toute modification majeure de stack (langage, framework
  principal, base de données) DOIT faire l'objet d'un ADR (Architecture Decision Record)
  approuvé en revue.
- **Migrations de base de données** : Elles DOIVENT être versionnées, réversibles et testées
  en isolation avant déploiement.
- **Variables d'environnement** : Un fichier `.env.example` DOIT rester à jour avec toutes
  les variables requises (sans valeurs sensibles).
- **Versioning d'API** : Les endpoints publics DOIVENT inclure une version dans l'URL
  (ex. `/api/v1/...`) ; une rupture de contrat impose une montée de version majeure.
- **Documentation des endpoints** : Tout endpoint DOIT être documenté (OpenAPI/Swagger ou
  équivalent) avant livraison en production.

## Processus de validation

Les portes de qualité suivantes DOIVENT être franchies avant toute fusion de branche :

- **CI au vert** : Lint, tests unitaires, tests d'intégration et audit de sécurité DOIVENT
  tous passer sans erreur.
- **Constitution Check** : Le `plan.md` de chaque fonctionnalité DOIT lister les principes
  applicables et documenter toute dérogation avec justification explicite.
- **Revue de sécurité** : Tout ajout ou modification d'endpoint ou de règle de permission
  DOIT être explicitement validé lors de la revue de code.
- **Vérification UX** : Pour tout changement d'interface, un test E2E ou une capture d'écran
  DOIT démontrer la conformité à la charte UX (principe VI).
- **Revue de traçabilité** : Toute nouvelle opération métier DOIT démontrer en revue que les
  événements d'audit correspondants sont bien émis (principe V).

## Governance

Cette constitution est le document de référence du projet TimePlanning. Elle prévaut sur
toute autre pratique ou convention ad hoc au sein de l'équipe.

**Amendements** :
- Tout amendement DOIT être soumis via un PR dédié décrivant l'impact sur les principes
  existants.
- Les changements de principe (MAJOR ou MINOR) DOIVENT être approuvés par au moins deux
  membres de l'équipe avant fusion.
- Un plan de migration DOIT accompagner tout changement incompatible (bump MAJOR).
- La version DOIT être incrémentée selon les règles sémantiques ci-dessous.

**Règles de versioning** :
- **MAJOR** : Suppression ou redéfinition incompatible d'un principe existant.
- **MINOR** : Ajout d'un principe ou extension significative de la portée d'un principe.
- **PATCH** : Clarifications, corrections de formulation, précisions non sémantiques.

**Conformité** :
- Les revues de code DOIVENT vérifier la conformité aux principes applicables à chaque PR.
- Toute dérogation DOIT être documentée dans le `plan.md` de la fonctionnalité concernée.
- Les violations non justifiées DOIVENT bloquer la fusion.

**Version**: 1.0.0 | **Ratifiée**: 2026-05-21 | **Dernière modification**: 2026-05-21
