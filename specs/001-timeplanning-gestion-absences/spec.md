# Spécification de fonctionnalité : TimePlanning — Gestion des Absences

**Branche** : `001-timeplanning-gestion-absences`
**Créée** : 2026-05-21
**Statut** : En implémentation
**Entrée** : Application interne de gestion des absences et de planning d'équipe TimePlanning

---

## Scénarios utilisateur & tests *(obligatoires)*

### User Story 1 — Salarié : déclarer et suivre une demande d'absence (Priorité : P1)

Un salarié peut créer une demande d'absence directement dans l'application, sans passer par email. Il choisit le type d'absence, les dates (avec option demi-journée), et soumet la demande. Il retrouve immédiatement sa demande dans la liste de ses demandes en cours avec un statut visible. Il peut annuler une demande tant qu'elle n'est pas validée.

**Pourquoi cette priorité** : C'est le cœur du produit. Sans cette capacité, aucun autre flux ne peut fonctionner. Elle élimine directement le problème de dispersion par email.

**Test indépendant** : Peut être entièrement testée en créant une demande, en vérifiant son apparition dans "mes demandes en cours" avec le bon statut, puis en l'annulant — sans nécessiter aucun autre flux.

**Scénarios d'acceptation** :

1. **Étant donné** un salarié connecté, **quand** il crée une demande d'absence avec un type valide, des dates valides et les soumet, **alors** la demande apparaît immédiatement dans sa liste avec le statut "En attente".
2. **Étant donné** une demande en attente, **quand** le salarié demande son annulation, **alors** la demande passe au statut "Annulée" et disparaît des traitements à venir.
3. **Étant donné** une période avec week-end et jour férié, **quand** le salarié saisit une absence couvrant ces jours, **alors** le nombre de jours calculé exclut automatiquement les week-ends, jours fériés et jours non travaillés du salarié.
4. **Étant donné** une absence déjà existante sur une période, **quand** le salarié tente de créer une nouvelle absence qui chevauche cette période, **alors** la création est bloquée avec un message explicite (sauf exception Ecole).
5. **Étant donné** un type d'absence quantifié, **quand** le salarié consulte ses demandes, **alors** il voit son compteur de consommation (jours pris, jours validés à venir, jours en attente).

---

### User Story 2 — Manager : valider ou refuser les demandes de son périmètre (Priorité : P1)

Un manager voit immédiatement les demandes en attente pour son équipe. Il peut valider ou refuser chaque demande. Un refus exige obligatoirement un motif. Il consulte le planning mensuel de son équipe et peut filtrer par sous-équipe. Il peut également saisir une absence pour un salarié de son périmètre si ce droit lui est accordé.

**Pourquoi cette priorité** : Sans validation, le flux d'absence est incomplet. C'est le deuxième élément critique du MVP.

**Test indépendant** : Testable indépendamment en validant puis refusant des demandes soumises par un salarié test, et en vérifiant que le refus sans motif est impossible.

**Scénarios d'acceptation** :

1. **Étant donné** une demande en attente dans son périmètre, **quand** le manager la valide, **alors** la demande passe au statut "Validée" et le salarié est notifié.
2. **Étant donné** une demande en attente, **quand** le manager tente de la refuser sans saisir de motif, **alors** l'action est bloquée et un message demande la saisie du motif.
3. **Étant donné** un manager qui est aussi salarié, **quand** sa propre demande d'absence remonte dans la file de validation, **alors** il ne peut pas la valider lui-même.
4. **Étant donné** un planning mensuel, **quand** le manager le consulte, **alors** toutes les absences de son périmètre sont affichées avec leur couleur et code de type.
5. **Étant donné** un manager avec plusieurs sous-équipes, **quand** il filtre par sous-équipe, **alors** seules les absences de cette sous-équipe sont affichées.

---

### User Story 3 — Workflow N+1 puis N+2 : double validation (Priorité : P2)

Certaines équipes sont configurées avec un workflow de double validation. Dans ce cas, la demande doit d'abord être validée par le N+1, puis par le N+2. Le N+1 reçoit l'email de dépôt. Le N+2 peut valider directement sans attendre le N+1. Le salarié voit un statut simple (en attente, validée, refusée).

**Pourquoi cette priorité** : Essentiel pour les équipes avec hiérarchie à deux niveaux, mais la valeur MVP peut être démontrée avec un workflow N+1 seul en premier.

**Test indépendant** : Testable avec une équipe configurée en double validation : vérifier que la demande passe bien par les deux étapes, que le N+2 peut court-circuiter, et que le salarié ne voit qu'un statut simplifié.

**Scénarios d'acceptation** :

1. **Étant donné** une équipe configurée en double validation, **quand** le N+1 valide une demande, **alors** la demande passe au statut intermédiaire "Validation N+2 en attente" et le N+2 en est informé.
2. **Étant donné** une équipe en double validation, **quand** le N+2 valide directement sans attendre le N+1, **alors** la demande est considérée comme validée à tous les niveaux.
3. **Étant donné** un workflow à double validation, **quand** le N+1 refuse une demande, **alors** la demande est refusée définitivement (le N+2 n'a pas à intervenir).
4. **Étant donné** un salarié dont l'équipe est en double validation, **quand** il consulte ses demandes, **alors** il voit uniquement "En attente", "Validée" ou "Refusée" — sans distinguer N+1 de N+2.

---

### User Story 4 — Salarié : modifier une absence validée (Priorité : P2)

Un salarié peut demander la modification d'une absence déjà validée. La modification crée une nouvelle version qui repasse par le circuit de validation. L'ancienne version reste la référence jusqu'à validation de la nouvelle.

**Pourquoi cette priorité** : Nécessaire pour gérer les cas courants de changement de plan, mais le produit reste utilisable sans cette story pour un premier déploiement.

**Test indépendant** : Testable en modifiant une absence validée, en vérifiant que l'ancienne version reste active pendant la revalidation, et que la nouvelle version remplace l'ancienne après approbation.

**Scénarios d'acceptation** :

1. **Étant donné** une absence validée, **quand** le salarié demande sa modification avec de nouvelles dates, **alors** une nouvelle version est créée avec le statut "En attente" et l'ancienne version reste active.
2. **Étant donné** une nouvelle version en attente, **quand** le manager la valide, **alors** la nouvelle version devient la référence et l'ancienne est archivée.
3. **Étant donné** une nouvelle version en attente, **quand** le manager la refuse, **alors** l'ancienne version validée reste la référence active et le salarié est informé.

---

### User Story 5 — RH : clôture mensuelle et export paie (Priorité : P2)

La RH peut clôturer une période mensuelle pour figer les données de paie. Toute validation survenant après la clôture génère une régularisation visible. La RH peut générer un export annuel au format Excel pour la paie. La RH peut également corriger une absence existante en cas d'erreur, avec conservation d'un historique complet.

**Pourquoi cette priorité** : Critique pour les équipes RH/paie mais n'impacte pas le flux quotidien des absences.

**Test indépendant** : Testable en clôturant un mois, en validant une absence après clôture, et en vérifiant l'apparition de la régularisation dans le suivi.

**Scénarios d'acceptation** :

1. **Étant donné** un mois non clôturé, **quand** la RH déclenche la clôture, **alors** la période est figée et toutes les absences validées à ce moment sont enregistrées comme données de paie officielles.
2. **Étant donné** un mois clôturé, **quand** une absence est validée après coup pour cette période, **alors** une régularisation est créée et visible dans le suivi des régularisations RH.
3. **Étant donné** une année de données, **quand** la RH demande l'export annuel paie, **alors** un fichier Excel est généré contenant toutes les absences validées de l'année par salarié, type et période.
4. **Étant donné** une absence contenant une erreur de saisie, **quand** la RH la corrige, **alors** un historique complet de la modification est conservé (valeur avant, valeur après, auteur, date).

---

### User Story 6 — Module récurrences : école et jours non travaillés (Priorité : P3)

Un module dédié permet de gérer les récurrences d'absences pour les alternants (Ecole) et les jours non travaillés contractuels. Ce module est distinct du flux standard et peut être alimenté manuellement, par import Excel, ou par génération périodique. Il est activable uniquement pour certains utilisateurs.

**Pourquoi cette priorité** : Nécessaire pour les alternants mais concerne un sous-ensemble d'utilisateurs. Peut être activé après la mise en production du flux principal.

**Test indépendant** : Testable en créant un patron de récurrence pour un alternant, en le générant sur un trimestre, et en vérifiant que les absences apparaissent dans le planning sans bloquer les autres demandes.

**Scénarios d'acceptation** :

1. **Étant donné** un utilisateur avec accès au module récurrences, **quand** il importe un fichier Excel de planning école, **alors** les absences sont créées pour les dates correspondantes avec le type "Ecole".
2. **Étant donné** une récurrence configurée, **quand** la génération périodique est déclenchée, **alors** les événements futurs sont créés jusqu'à la fin de la période définie.
3. **Étant donné** une absence Ecole qui chevauche une autre absence, **quand** la récurrence est injectée, **alors** un avertissement est affiché mais l'injection n'est pas bloquée.
4. **Étant donné** un historique d'injections, **quand** un utilisateur consulte le module, **alors** il voit la liste de toutes les injections passées avec date, source et nombre d'événements créés.

---

### User Story 7 — Admin technique : paramétrage de l'application (Priorité : P3)

L'admin technique peut gérer les utilisateurs, les équipes, la hiérarchie N+1/N+2 et les types d'absence. Il accède à la structure applicative sans voir les données personnelles d'absences des salariés.

**Pourquoi cette priorité** : Nécessaire pour le déploiement initial mais ne génère pas de valeur métier quotidienne une fois en place.

**Test indépendant** : Testable en ajoutant un nouveau type d'absence depuis le panneau d'administration, puis en vérifiant qu'il apparaît dans les options de création de demande d'un salarié.

**Scénarios d'acceptation** :

1. **Étant donné** un admin technique connecté, **quand** il crée un nouveau type d'absence avec un code, un libellé, une couleur et ses règles de validation, **alors** ce type est immédiatement disponible à la saisie par les rôles autorisés.
2. **Étant donné** la hiérarchie d'une équipe, **quand** l'admin affecte un N+1 et un N+2 à une équipe, **alors** le workflow de validation de cette équipe est mis à jour automatiquement.
3. **Étant donné** un admin technique, **quand** il consulte le module administration, **alors** il n'a accès à aucune donnée d'absence individuelle des salariés.

---

### Cas limites

- Que se passe-t-il si un manager est absent et qu'une demande reste en attente sans traitement ? (Pas de relance automatique définie en V1)
- Comment le système réagit-il si N+1 et N+2 traitent quasi simultanément la même demande en double validation ?
- Que se passe-t-il si un salarié pose une demi-journée sur un jour où une autre absence est déjà saisie pour l'autre demi-journée ?
- Que se passe-t-il si l'admin désactive un type d'absence utilisé par des demandes en cours non encore validées ?
- Comment le calcul de jours gère-t-il une absence qui enjambe le réveillon et le Jour de l'An (changement d'année civile) ?
- Que se passe-t-il si un salarié change d'équipe (et donc de N+1) pendant qu'une demande est en cours de validation ?

---

## Exigences *(obligatoires)*

### Exigences fonctionnelles

#### Module Demandes d'absence (EF-001 à EF-008)

- **EF-001** : Le système DOIT permettre à un salarié de créer une demande d'absence en précisant le type, la date de début, la date de fin, et l'option demi-journée au début et/ou à la fin.
- **EF-002** : Le système DOIT calculer automatiquement le nombre de jours ouvrés en excluant les week-ends, les jours fériés et les jours non travaillés propres au salarié.
- **EF-003** : Le système DOIT gérer les demi-journées au début et à la fin d'une période dans le calcul du décompte (une demi-journée compte pour 0,5 jour).
- **EF-004** : Le système DOIT bloquer la création d'une absence standard qui chevauche une absence existante du même salarié, et afficher un message explicite identifiant le conflit.
- **EF-005** : Le système DOIT autoriser une absence de type Ecole à chevaucher d'autres absences, avec affichage d'un avertissement non bloquant.
- **EF-006** : Le système DOIT permettre à un salarié d'annuler une demande non encore validée, sans nécessiter de justification.
- **EF-007** : Le système DOIT permettre à un salarié de demander la modification d'une absence validée, ce qui crée une nouvelle version soumise à revalidation.
- **EF-008** : Le système DOIT maintenir l'ancienne version validée comme référence active tant que la nouvelle version n'a pas été approuvée par le circuit de validation.

#### Module Validation (EF-009 à EF-014)

- **EF-009** : Le système DOIT router chaque demande vers le N+1 du salarié selon le mode de validation configuré pour son équipe (N+1 seul, ou N+1 puis N+2).
- **EF-010** : Le système DOIT empêcher tout manager de valider ou refuser sa propre demande d'absence.
- **EF-011** : Le système DOIT exiger la saisie d'un motif pour tout refus, sans exception — le refus sans motif est techniquement impossible.
- **EF-012** : Dans un workflow N+1 puis N+2, le système DOIT permettre au N+2 de valider directement une demande sans attendre le N+1.
- **EF-013** : Le système DOIT permettre au manager d'annuler une absence validée en saisissant une justification obligatoire.
- **EF-014** : Le système DOIT permettre à un manager disposant des droits appropriés de saisir une absence pour le compte d'un salarié de son périmètre.

#### Module Planning (EF-015 à EF-017)

- **EF-015** : Le système DOIT proposer un planning mensuel affichant toutes les absences de l'équipe du manager, avec la couleur et le code du type pour chaque absence.
- **EF-016** : Le planning DOIT afficher le mois courant par défaut et permettre la navigation d'un mois à l'autre.
- **EF-017** : Le planning DOIT permettre le filtrage par équipe et par sous-équipe, selon les droits de l'utilisateur connecté.

#### Module Compteurs (EF-018 à EF-020)

- **EF-018** : Le système DOIT afficher un compteur de consommation pour chaque type d'absence configuré comme quantifié, avec trois valeurs distinctes : jours pris dans le passé, jours validés à venir, jours en attente de validation.
- **EF-019** : Le système NE DOIT PAS gérer les soldes de droits acquis en V1 — seule la consommation est suivie.
- **EF-020** : Les compteurs DOIVENT être visibles par le salarié pour lui-même, par le manager pour son périmètre, et par la RH. L'admin technique n'y a pas accès.

#### Module RH et Paie (EF-021 à EF-027)

- **EF-021** : Le système DOIT permettre à la RH de consulter toutes les absences avec des filtres par salarié, équipe, type d'absence et période.
- **EF-022** : Le système DOIT permettre à la RH de saisir des absences spéciales (arrêt maladie, congé maternité, congé spécial, etc.) pour tout salarié.
- **EF-023** : Le système DOIT permettre à la RH de corriger une absence existante, avec conservation d'une trace complète et immuable (valeur avant, valeur après, auteur, date et heure de modification).
- **EF-024** : Le système DOIT permettre à la RH de clôturer une période mensuelle pour figer les données de paie, en une seule action sans intervention technique.
- **EF-025** : Le système DOIT détecter automatiquement toute absence validée après la clôture d'une période et la signaler comme régularisation sans action manuelle de la RH.
- **EF-026** : Le système DOIT permettre à la RH de consulter la liste des régularisations, filtrée par période.
- **EF-027** : Le système DOIT générer un export Excel annuel de toutes les absences validées, structuré par salarié, type d'absence et période, exploitable pour le traitement de la paie.

#### Module Récurrences (EF-028 à EF-030)

- **EF-028** : Le module récurrences DOIT permettre la saisie unitaire, l'import Excel et la génération périodique d'événements d'absence de type Ecole ou Jours non travaillés.
- **EF-029** : Le module récurrences DOIT conserver un historique de toutes les injections avec date, source (saisie / import / génération), périmètre concerné et nombre d'événements créés.
- **EF-030** : Le module récurrences DOIT être activable uniquement pour des utilisateurs explicitement désignés dans la configuration.

#### Module Administration (EF-031 à EF-035)

- **EF-031** : Le système DOIT permettre la gestion des utilisateurs (création, modification, désactivation) et leur affectation à des équipes avec un rôle.
- **EF-031b** : Le système DOIT permettre à l'admin technique (et au manager avec délégation explicite) de configurer le planning contractuel d'un salarié : jours non travaillés récurrents (ex. mercredi pour un mi-temps), avec date d'effet et date de fin. Ces jours sont utilisés par EF-002 dans le calcul des jours ouvrés.
- **EF-032** : Le système DOIT permettre la gestion des équipes et sous-équipes, incluant la définition de la hiérarchie N+1 et N+2 pour chaque équipe.
- **EF-033** : Le système DOIT permettre la configuration du mode de validation par équipe (N+1 seul ou N+1 puis N+2), avec un mode global pouvant être surchargé équipe par équipe.
- **EF-034** : Le système DOIT permettre la gestion complète des types d'absence : code, libellé, couleur, obligation de validation manager, obligation de justificatif, rôles autorisés à saisir, quantification dans les compteurs, remise à zéro annuelle (si activée : les compteurs ne comptabilisent que les absences de l'année civile en cours — filtre de date, pas de reset persistant), statut actif/inactif.
- **EF-035** : L'admin technique NE DOIT avoir accès à aucune donnée individuelle d'absence des salariés.

#### Notifications (EF-036 à EF-037)

- **EF-036** : Le système DOIT envoyer un email de notification lors de chaque événement : création de demande, validation, refus, annulation, modification.
- **EF-037** : Dans un workflow N+1 puis N+2, seul le N+1 DOIT recevoir l'email initial de dépôt d'une demande.

#### Traçabilité (EF-038)

- **EF-038** : Le système DOIT conserver un historique complet et immuable de tous les événements sur chaque absence : création, validations, refus, annulations, modifications, corrections RH, clôtures, régularisations — avec l'auteur, la date et les valeurs avant/après pour chaque action.

### Entités clés

- **Utilisateur** : Personne avec un ou plusieurs rôles (Salarié, Manager, RH, Admin), membre d'une ou plusieurs équipes, avec une hiérarchie N+1 et/ou N+2 définie.
- **Équipe** : Groupe de salariés rattachés à un ou plusieurs managers, avec un mode de validation propre (N+1 seul ou N+1 puis N+2).
- **TypeAbsence** : Référentiel paramétrable définissant les règles de chaque catégorie d'absence (code, libellé, couleur, règles de validation, quantification, remise à zéro, rôles autorisés, statut actif).
- **Absence** : Demande d'absence d'un salarié avec son type, son statut courant, et l'ensemble de ses versions successives.
- **VersionAbsence** : Une version spécifique d'une absence avec ses dates, son statut propre (en attente, validée N+1, validée, refusée, annulée) et ses événements de traitement associés.
- **EvenementAudit** : Trace immuable de chaque action sur une absence (auteur, type d'action, date et heure, valeur avant, valeur après).
- **PeriodeCloture** : Mois figé par la RH, avec date de clôture et liste des absences rattachées à cette période.
- **Regularisation** : Absence validée après clôture d'une période, identifiée automatiquement et rattachée à la période concernée pour traitement en paie complémentaire.
- **RecurrencePattern** : Patron de récurrence pour les absences Ecole ou Jours non travaillés, avec source (saisie manuelle, import, génération) et historique d'injections.
- **CompteurAbsence** : Agrégat calculé dynamiquement par type quantifié et par salarié (jours pris passés, jours validés à venir, jours en attente). Aucun état persistant — les valeurs sont recalculées à chaque consultation depuis les données d'absence existantes. Quand `TypeAbsence.resetAnnuel=true`, le calcul est restreint à l'année civile en cours (filtre de date dans la requête de calcul, pas de service de reset périodique).

---

## Critères de succès *(obligatoires)*

### Résultats mesurables

- **CS-001** : Un salarié peut créer et soumettre une demande d'absence en moins de 2 minutes, sans formation préalable.
- **CS-002** : Le calcul automatique des jours ouvrés est exact dans 100% des cas testés (week-ends, jours fériés, jours non travaillés, demi-journées, changement d'année).
- **CS-003** : Un manager peut traiter (valider ou refuser) une demande en attente en moins de 1 minute depuis la réception de la notification.
- **CS-004** : Zéro demande d'absence soumise ne peut être perdue — toute demande est traçable de la création à la décision finale, avec son historique complet.
- **CS-005** : La RH peut générer l'export annuel paie en moins de 30 secondes, quelle que soit la volumétrie annuelle.
- **CS-006** : La clôture mensuelle est opérée par la RH en une seule action, sans intervention de l'équipe technique.
- **CS-007** : Toute absence validée après clôture est automatiquement identifiée comme régularisation, sans action manuelle de la RH.
- **CS-008** : Un admin technique peut ajouter un nouveau type d'absence et le rendre disponible à la saisie en moins de 5 minutes.
- **CS-009** : Le planning mensuel est lisible sans formation — 90% des managers comprennent l'état des absences de leur équipe au premier coup d'œil lors d'un test utilisateur.
- **CS-010** : L'ensemble des événements sur une absence (création, modifications, validations, corrections RH) est accessible dans un historique complet, consultable par la RH à tout moment.

---

## Hypothèses

- Chaque salarié appartient à au moins une équipe avec un N+1 identifié. Un salarié sans N+1 configuré ne peut pas soumettre de demande nécessitant validation — ce cas doit être bloqué avec un message explicite.
- Les jours fériés utilisés dans le calcul sont les jours fériés légaux français. Les éventuels jours fériés locaux ou conventionnels sont gérés via le mécanisme "jours non travaillés" configuré sur le profil du salarié.
- La notion de "jours non travaillés" propres à un salarié est configurée au niveau de son profil ou de son type de contrat et ne change pas de façon fréquente.
- L'authentification des utilisateurs s'appuie sur **Google OAuth2 / OIDC** (Authorization Server Google) → JWT Bearer token validé par Spring Security OAuth2 Resource Server. L'application ne gère pas les mots de passe en propre. *Décision technique entérinée en phase de planification — voir plan.md §Contexte technique.*
- L'export paie annuel suit le format Excel défini avec l'équipe RH en phase de planification. Structure retenue : une ligne par absence validée, colonnes : `Matricule`, `Nom`, `Prénom`, `Équipe`, `Type d'absence` (code + libellé), `Date début`, `Date fin`, `Nb jours ouvrés`, `Statut`, `Période de paie`. Un onglet par mois ; une feuille de synthèse annuelle. Ce format DOIT être validé par la RH avant le démarrage de T115.
- Les justificatifs (arrêts maladie papier, etc.) ne sont pas stockés dans l'application — leur gestion reste hors périmètre, conformément aux décisions verrouillées du cahier des charges.
- L'application est accessible via navigateur web sur desktop. Le niveau d'optimisation pour les écrans de planning et RH sur mobile reste une question ouverte à trancher avant la planification technique.
- La stratégie de relance automatique des validations en attente (rappels email) n'est pas définie en V1 — les managers sont supposés consulter leurs files d'attente sans mécanisme de relance automatique.
- En cas d'action quasi-simultanée de N+1 et N+2 sur la même demande dans un workflow double validation, la première action enregistrée prévaut. La gestion de conflits concurrents avancée est hors périmètre V1.
- Par défaut, seul l'admin technique peut créer et modifier des utilisateurs. Les droits éventuels de gestion des utilisateurs délégués aux managers sont à préciser avant la planification technique (question ouverte du cahier des charges).
