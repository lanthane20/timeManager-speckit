package fr.timemanager.domain.shared;

/**
 * Représente les options demi-journée pour le début et la fin d'une absence. NONE = journée
 * entière, AM = matin uniquement, PM = après-midi uniquement.
 */
public record HalfDay(HalfDayType start, HalfDayType end) {

  public static final HalfDay NONE = new HalfDay(HalfDayType.NONE, HalfDayType.NONE);

  public HalfDay {
    if (start == null) {
      start = HalfDayType.NONE;
    }
    if (end == null) {
      end = HalfDayType.NONE;
    }
  }

  public boolean hasHalfDayStart() {
    return start != HalfDayType.NONE;
  }

  public boolean hasHalfDayEnd() {
    return end != HalfDayType.NONE;
  }

  public double startAdjustment() {
    return start == HalfDayType.PM ? -0.5 : 0.0;
  }

  public double endAdjustment() {
    return end == HalfDayType.AM ? -0.5 : 0.0;
  }
}
