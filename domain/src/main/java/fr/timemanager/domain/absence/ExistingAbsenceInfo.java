package fr.timemanager.domain.absence;

import fr.timemanager.domain.shared.DateRange;

/** DTO léger pour passer les infos de chevauchement au checker. */
public record ExistingAbsenceInfo(DateRange dateRange, boolean allowsOverlap) {}
