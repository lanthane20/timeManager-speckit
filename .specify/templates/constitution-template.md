# Constitution [NOM_DU_PROJET]
<!-- Exemples : Constitution TimePlanning, Constitution SpecKit, etc. -->

## Principes fondamentaux

### [NOM_PRINCIPE_1]
<!-- Exemple : I. Architecture modulaire -->
[DESCRIPTION_PRINCIPE_1]
<!-- Exemple : Chaque fonctionnalité démarre comme une bibliothèque autonome ;
     les bibliothèques doivent être indépendantes, testables et documentées ;
     un objectif clair est requis — pas de bibliothèques purement organisationnelles. -->

### [NOM_PRINCIPE_2]
<!-- Exemple : II. Interface CLI -->
[DESCRIPTION_PRINCIPE_2]
<!-- Exemple : Chaque bibliothèque expose ses fonctionnalités via CLI ;
     protocole texte entrée/sortie : stdin/args → stdout, erreurs → stderr ;
     formats JSON et lisible par un humain requis. -->

### [NOM_PRINCIPE_3]
<!-- Exemple : III. Tests en premier (NON NÉGOCIABLE) -->
[DESCRIPTION_PRINCIPE_3]
<!-- Exemple : TDD obligatoire : tests écrits → approuvés → échouent → puis implémentation ;
     cycle Rouge-Vert-Refactoring strictement respecté. -->

### [NOM_PRINCIPE_4]
<!-- Exemple : IV. Tests d'intégration -->
[DESCRIPTION_PRINCIPE_4]
<!-- Exemple : Périmètres nécessitant des tests d'intégration : nouveaux contrats de
     bibliothèque, changements de contrat, communication inter-services, schémas partagés. -->

### [NOM_PRINCIPE_5]
<!-- Exemple : V. Observabilité, VI. Versioning & ruptures de contrat, VII. Simplicité -->
[DESCRIPTION_PRINCIPE_5]
<!-- Exemple : Les entrées/sorties texte garantissent la debuggabilité ; journalisation
     structurée requise. Ou : format MAJOR.MINOR.BUILD. Ou : commencer simple, YAGNI. -->

## [NOM_SECTION_2]
<!-- Exemple : Contraintes supplémentaires, Exigences de sécurité, Standards de performance -->

[CONTENU_SECTION_2]
<!-- Exemple : Exigences de stack technique, standards de conformité, politiques de déploiement. -->

## [NOM_SECTION_3]
<!-- Exemple : Processus de développement, Processus de revue, Portes de qualité -->

[CONTENU_SECTION_3]
<!-- Exemple : Exigences de revue de code, portes de test, processus d'approbation de déploiement. -->

## Gouvernance
<!-- Exemple : La constitution prévaut sur toutes les autres pratiques ;
     les amendements nécessitent documentation, approbation, plan de migration. -->

[RÈGLES_DE_GOUVERNANCE]
<!-- Exemple : Tous les PR/revues doivent vérifier la conformité ; la complexité doit être
     justifiée ; utiliser [FICHIER_DE_GUIDANCE] pour le guidage de développement en runtime. -->

**Version** : [VERSION_CONSTITUTION] | **Ratifiée** : [DATE_RATIFICATION] | **Dernière modification** : [DATE_DERNIER_AMENDEMENT]
<!-- Exemple : Version : 2.1.1 | Ratifiée : 2025-06-13 | Dernière modification : 2025-07-16 -->
