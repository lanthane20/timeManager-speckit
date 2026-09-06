/** Statut simplifié exposé par l'API (EF-007) — terminologie identique à la spec et à l'UI. */
export type AbsenceSimplifiedStatus = 'EN_ATTENTE' | 'VALIDEE' | 'REFUSEE' | 'ANNULEE';

/** Option demi-journée : NONE = journée entière, AM = matin, PM = après-midi. */
export type HalfDayType = 'NONE' | 'AM' | 'PM';

export interface Absence {
  id: string;
  employeeId: string;
  absenceTypeId: string;
  absenceTypeLabel: string;
  absenceTypeColor: string | null;
  startDate: string;
  endDate: string;
  workingDays: number;
  status: AbsenceSimplifiedStatus;
  createdAt: string;
}

export interface CreateAbsenceRequest {
  absenceTypeId: string;
  startDate: string;
  endDate: string;
  halfDayStart: HalfDayType;
  halfDayEnd: HalfDayType;
}

export interface AbsenceType {
  id: string;
  code: string;
  label: string;
  color: string | null;
}

/**
 * Compteur d'absence (EF-018). Conformément à EF-019, aucun solde ni droit acquis n'est exposé :
 * seuls les jours consommés, validés à venir et en attente.
 */
export interface AbsenceCounter {
  employeeId: string;
  absenceTypeId: string;
  daysTakenPast: number;
  daysValidatedFuture: number;
  daysPending: number;
}
