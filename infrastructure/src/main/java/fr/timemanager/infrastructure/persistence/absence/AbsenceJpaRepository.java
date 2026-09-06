package fr.timemanager.infrastructure.persistence.absence;

import fr.timemanager.domain.absence.AbsenceSimplifiedStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbsenceJpaRepository extends JpaRepository<AbsenceEntity, UUID> {

  List<AbsenceEntity> findByEmployeeId(UUID employeeId);

  List<AbsenceEntity> findByTeamIdAndSimplifiedStatus(
      UUID teamId, AbsenceSimplifiedStatus simplifiedStatus);

  @Query(
      """
      SELECT DISTINCT a FROM AbsenceEntity a
        JOIN a.versions v
      WHERE a.employeeId = :employeeId
        AND v.active = true
        AND v.startDate <= :endDate
        AND v.endDate >= :startDate
      """)
  List<AbsenceEntity> findOverlapping(
      @Param("employeeId") UUID employeeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /** EF-021 : recherche RH — chaque filtre nul est neutre. */
  @Query(
      """
      SELECT DISTINCT a FROM AbsenceEntity a
        JOIN a.versions v
      WHERE v.active = true
        AND (:employeeId IS NULL OR a.employeeId = :employeeId)
        AND (:teamId IS NULL OR a.teamId = :teamId)
        AND (:absenceTypeId IS NULL OR a.absenceTypeId = :absenceTypeId)
        AND (:from IS NULL OR v.endDate >= :from)
        AND (:to IS NULL OR v.startDate <= :to)
      """)
  List<AbsenceEntity> search(
      @Param("employeeId") UUID employeeId,
      @Param("teamId") UUID teamId,
      @Param("absenceTypeId") UUID absenceTypeId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /** EF-027 : absences validées dont la version active chevauche l'année demandée. */
  @Query(
      """
      SELECT DISTINCT a FROM AbsenceEntity a
        JOIN a.versions v
      WHERE v.active = true
        AND a.simplifiedStatus = fr.timemanager.domain.absence.AbsenceSimplifiedStatus.VALIDEE
        AND v.endDate >= :from
        AND v.startDate <= :to
      """)
  List<AbsenceEntity> findValidatedBetween(
      @Param("from") LocalDate from, @Param("to") LocalDate to);
}
