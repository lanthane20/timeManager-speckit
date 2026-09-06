import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { Absence, AbsenceCounter } from '../../my-absences/models/absence.model';

/** T083 — Actions du manager sur les demandes de son périmètre (EF-009 à EF-014). */
@Injectable({ providedIn: 'root' })
export class ManagerAbsenceService {
  private readonly api = inject(ApiService);

  getPendingAbsences(): Observable<Absence[]> {
    return this.api.get<Absence[]>('/v1/manager/absences/pending');
  }

  /** EF-012 : demandes déjà validées par le N+1 et en attente du N+2. */
  getPendingSecondLevelAbsences(): Observable<Absence[]> {
    return this.api.get<Absence[]>('/v1/manager/absences/pending-n2');
  }

  validate(absenceId: string): Observable<void> {
    return this.api.put<void>(`/v1/manager/absences/${absenceId}/validate`, {});
  }

  /** EF-012 : validation de second niveau, réservée au N+2 de l'équipe. */
  validateN2(absenceId: string): Observable<void> {
    return this.api.put<void>(`/v1/manager/absences/${absenceId}/validate-n2`, {});
  }

  /** EF-011 : le motif est obligatoire — l'API refuse un motif vide. */
  refuse(absenceId: string, reason: string): Observable<void> {
    return this.api.put<void>(`/v1/manager/absences/${absenceId}/refuse`, { reason });
  }

  getEmployeeCounters(employeeId: string): Observable<AbsenceCounter[]> {
    return this.api.get<AbsenceCounter[]>(`/v1/manager/employees/${employeeId}/counters`);
  }
}
