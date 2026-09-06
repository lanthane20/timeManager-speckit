package fr.timemanager.domain.user;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EF-031b : planning contractuel d'un salarié — jours travaillés récurrents, avec date d'effet et
 * date de fin optionnelle. Les jours non travaillés qui en découlent alimentent le calcul des jours
 * ouvrés (EF-002).
 */
public record WorkSchedule(
    UUID id,
    UUID userId,
    Set<DayOfWeek> workingDays,
    LocalDate effectiveFrom,
    LocalDate effectiveUntil,
    Instant createdAt) {

  private static final Set<DayOfWeek> FULL_WEEK =
      EnumSet.of(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY);

  public WorkSchedule {
    if (userId == null) {
      throw new IllegalArgumentException("userId obligatoire");
    }
    if (effectiveFrom == null) {
      throw new IllegalArgumentException("effectiveFrom obligatoire");
    }
    if (workingDays == null || workingDays.isEmpty()) {
      throw new IllegalArgumentException("Le planning doit comporter au moins un jour travaillé");
    }
    if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
      throw new IllegalArgumentException("effectiveUntil doit être postérieure à effectiveFrom");
    }
    workingDays = Set.copyOf(workingDays);
  }

  /** Le planning s'applique-t-il à cette date ? Une date d'effet future ne s'applique pas. */
  public boolean appliesOn(LocalDate date) {
    return !date.isBefore(effectiveFrom)
        && (effectiveUntil == null || !date.isAfter(effectiveUntil));
  }

  /** Jours de la semaine non travaillés contractuellement (hors week-end). */
  public Set<DayOfWeek> nonWorkingDays() {
    return FULL_WEEK.stream()
        .filter(day -> !workingDays.contains(day))
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
  }

  /** Sérialisation du motif persisté, par exemple {@code MONDAY,TUESDAY,THURSDAY,FRIDAY}. */
  public String toPattern() {
    return workingDays.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
  }

  public static Set<DayOfWeek> parsePattern(String pattern) {
    if (pattern == null || pattern.isBlank()) {
      return Set.copyOf(FULL_WEEK);
    }
    List<DayOfWeek> days =
        Arrays.stream(pattern.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(DayOfWeek::valueOf)
            .toList();
    return days.isEmpty() ? Set.copyOf(FULL_WEEK) : Set.copyOf(days);
  }
}
