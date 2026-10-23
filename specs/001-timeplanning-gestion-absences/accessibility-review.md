# Revue d'accessibilité WCAG 2.1 AA — TimePlanning

**Tâche** : T142 · **Périmètre** : `frontend/src/app/features/**` et le shell applicatif
**Référence** : Constitution Principe VI (UX simple et lisible)

---

## Méthode

Relecture manuelle des gabarits Angular et des styles associés, écran par écran, au regard des
critères WCAG 2.1 niveau AA applicables à une application de gestion : perceptibilité (1.1, 1.3, 1.4),
utilisabilité au clavier (2.1, 2.4), compréhension (3.2, 3.3) et robustesse (4.1).

Aucun outil d'audit automatique n'est exécuté en CI à ce stade : l'analyse est manuelle et les
corrections ont été appliquées directement dans le code.

---

## Écarts corrigés

| Critère | Écart constaté | Correction appliquée |
|---------|----------------|----------------------|
| 3.1.1 Langue de la page | `index.html` déclarait `lang="en"` et le titre « Frontend » | `lang="fr"` et titre « TimePlanning — Gestion des absences » |
| 1.4.3 Contraste | Bandeau `#1976d2` avec texte blanc : 4,6:1, à la limite du seuil | Bandeau porté à `#0d47a1` (8,6:1) |
| 1.4.1 Utilisation de la couleur | L'entrée de navigation active n'était signalée que par une couleur de fond | Ajout d'un soulignement sur l'état actif |
| 1.4.1 Utilisation de la couleur | Le planning mensuel ne restituait l'absence que par la couleur de la cellule | Ajout du code du type en surimpression et d'un libellé textuel lu par les lecteurs d'écran |
| 1.3.1 Information et relations | Les cellules du planning n'avaient pas d'équivalent textuel | `aria-label` « Salarié, jour mois : type — statut » sur chaque cellule |
| 2.4.1 Contournement de blocs | Aucun lien d'évitement vers le contenu | Ajout d'un lien « Aller au contenu principal » visible à la prise de focus |
| 2.4.7 Visibilité du focus | Focus clavier peu visible sur le bandeau sombre | `outline` blanc de 3 px sur les liens de navigation, le bouton de déconnexion et le lien d'évitement |
| 4.1.2 Nom, rôle, valeur | `<nav>` sans nom accessible | `aria-label="Navigation principale"` |
| 4.1.2 Nom, rôle, valeur | Bouton de déconnexion sans `type` explicite dans un contexte de formulaire | `type="button"` |

---

## Points déjà conformes

- **1.3.1** : tous les tableaux utilisent `<th scope="col">` et `<th scope="row">` ; le planning
  possède une légende (`<caption>`) réservée aux lecteurs d'écran.
- **3.3.2** : chaque champ de formulaire possède un `<label for>` explicitement associé.
- **3.3.1 / 3.3.3** : les erreurs sont annoncées en texte (`p-message severity="error"`), jamais par
  la seule couleur, et le refus sans motif affiche un message explicite plutôt qu'un simple blocage.
- **1.4.10 / 1.4.4** : mise en page en `grid` avec unités relatives ; les tableaux passent en mode
  empilé sous 768 px (`responsiveLayout="stack"`), le planning défile horizontalement dans son
  propre conteneur sans provoquer de défilement du corps de page.
- **2.1.1** : aucun comportement ne dépend du survol seul — les infobulles du planning doublent une
  information déjà disponible en texte.
- **4.1.3** : les états de traitement sont annoncés par un `p-progressSpinner` doté d'un
  `ariaLabel`, et le point de retour OIDC expose son état via `role="status"`.

---

## Actions restantes

| # | Action | Raison |
|---|--------|--------|
| 1 | Audit automatisé (axe-core) branché sur les scénarios Playwright | Détecter les régressions à chaque exécution de la CI |
| 2 | Vérification des composants PrimeNG avec un lecteur d'écran réel (NVDA, VoiceOver) | Les composants tiers ne sont pas entièrement maîtrisés par le projet |
| 3 | Test de navigation clavier complet sur les boîtes de dialogue PrimeNG (piège de focus) | Comportement dépendant de la version de la bibliothèque |

Ces trois actions demandent soit un outillage supplémentaire, soit une vérification humaine avec un
matériel d'assistance : elles ne peuvent pas être validées par la seule relecture du code.
