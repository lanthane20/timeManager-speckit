package fr.timemanager.application.counter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fr.timemanager.application.absence.AbsenceTypeRepository;
import fr.timemanager.domain.absence.AbsenceVersionStatus;
import fr.timemanager.domain.absencetype.AbsenceType;
import fr.timemanager.domain.counter.CompteurAbsence;
import fr.timemanager.domain.user.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * EF-034 : la « remise à zéro annuelle » est un simple filtre de date dans le calcul des compteurs
 * — aucun état persistant, aucun job, aucune table supplémentaire.
 */
class GetAbsenceCountersResetAnnuelTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 6);

  private final CounterRepository counterRepository = mock(CounterRepository.class);
  private final AbsenceTypeRepository absenceTypeRepository = mock(AbsenceTypeRepository.class);
  private final GetAbsenceCounters useCase =
      new GetAbsenceCounters(counterRepository, absenceTypeRepository);

  private final UUID employeeId = UUID.randomUUID();
  private final UUID annualResetTypeId = UUID.randomUUID();
  private final UUID cumulativeTypeId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    Mockito.when(absenceTypeRepository.findById(annualResetTypeId))
        .thenReturn(Optional.of(type(annualResetTypeId, "CP", true, true)));
    Mockito.when(absenceTypeRepository.findById(cumulativeTypeId))
        .thenReturn(Optional.of(type(cumulativeTypeId, "RECUP", true, false)));
  }

  @Test
  void resetAnnuelActif_seulesLesAbsencesDeLAnneeCouranteSontComptees() {
    Mockito.when(counterRepository.findCounterEntries(employeeId))
        .thenReturn(
            List.of(
                entry(annualResetTypeId, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6), "5.0"),
                entry(
                    annualResetTypeId, LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 7), "5.0")));

    CompteurAbsence counter = onlyCounter();

    assertThat(counter.absenceTypeId()).isEqualTo(annualResetTypeId);
    assertThat(counter.daysTakenPast()).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void resetAnnuelInactif_toutesLesAnneesSontComptees() {
    Mockito.when(counterRepository.findCounterEntries(employeeId))
        .thenReturn(
            List.of(
                entry(cumulativeTypeId, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6), "5.0"),
                entry(
                    cumulativeTypeId, LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 7), "5.0")));

    CompteurAbsence counter = onlyCounter();

    assertThat(counter.daysTakenPast()).isEqualByComparingTo(new BigDecimal("10.0"));
  }

  @Test
  void absenceAChevalSurLeNouvelAn_estImputeeALAnneeDeSaDateDeDebut() {
    Mockito.when(counterRepository.findCounterEntries(employeeId))
        .thenReturn(
            List.of(
                entry(
                    annualResetTypeId,
                    LocalDate.of(2025, 12, 29),
                    LocalDate.of(2026, 1, 2),
                    "5.0")));

    assertThat(useCase.execute(employeeId, TODAY)).isEmpty();
  }

  @Test
  void lesTroisValeursDistinctesSontDistinguees() {
    Mockito.when(counterRepository.findCounterEntries(employeeId))
        .thenReturn(
            List.of(
                entry(annualResetTypeId, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6), "5.0"),
                new AbsenceCounterEntry(
                    annualResetTypeId,
                    LocalDate.of(2026, 11, 2),
                    LocalDate.of(2026, 11, 6),
                    new BigDecimal("5.0"),
                    AbsenceVersionStatus.VALIDEE),
                new AbsenceCounterEntry(
                    annualResetTypeId,
                    LocalDate.of(2026, 12, 7),
                    LocalDate.of(2026, 12, 8),
                    new BigDecimal("2.0"),
                    AbsenceVersionStatus.EN_ATTENTE)));

    CompteurAbsence counter = onlyCounter();

    assertThat(counter.daysTakenPast()).isEqualByComparingTo(new BigDecimal("5.0"));
    assertThat(counter.daysValidatedFuture()).isEqualByComparingTo(new BigDecimal("5.0"));
    assertThat(counter.daysPending()).isEqualByComparingTo(new BigDecimal("2.0"));
  }

  @Test
  void unTypeNonQuantifie_nApparaitPasDansLesCompteurs() {
    UUID nonQuantifiedTypeId = UUID.randomUUID();
    Mockito.when(absenceTypeRepository.findById(nonQuantifiedTypeId))
        .thenReturn(Optional.of(type(nonQuantifiedTypeId, "ECOLE", false, false)));
    Mockito.when(counterRepository.findCounterEntries(employeeId))
        .thenReturn(
            List.of(
                entry(
                    nonQuantifiedTypeId,
                    LocalDate.of(2026, 3, 2),
                    LocalDate.of(2026, 3, 6),
                    "5.0")));

    assertThat(useCase.execute(employeeId, TODAY)).isEmpty();
  }

  private CompteurAbsence onlyCounter() {
    List<CompteurAbsence> counters = useCase.execute(employeeId, TODAY);
    assertThat(counters).hasSize(1);
    return counters.getFirst();
  }

  private AbsenceCounterEntry entry(
      UUID absenceTypeId, LocalDate start, LocalDate end, String workingDays) {
    return new AbsenceCounterEntry(
        absenceTypeId, start, end, new BigDecimal(workingDays), AbsenceVersionStatus.VALIDEE);
  }

  private AbsenceType type(UUID id, String code, boolean quantified, boolean annualReset) {
    return new AbsenceType(
        id,
        code,
        code,
        "#4CAF50",
        true,
        false,
        Set.of(UserRole.SALARIE),
        quantified,
        annualReset,
        false,
        true);
  }
}
