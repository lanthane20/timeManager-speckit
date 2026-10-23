package fr.timemanager.domain.recurrence;

/** EF-029 : origine d'une injection d'absences récurrentes. */
public enum InjectionSource {
  /** Saisie unitaire dans le module récurrences. */
  SAISIE,
  /** Import d'un fichier Excel de planning école. */
  IMPORT,
  /** Génération périodique à partir d'un patron. */
  GENERATION
}
