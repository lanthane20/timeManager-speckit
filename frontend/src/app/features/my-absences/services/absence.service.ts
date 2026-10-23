import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import {
  Absence,
  AbsenceCounter,
  AbsenceType,
  CreateAbsenceRequest,
} from '../models/absence.model';

/** T063 — Accès REST aux demandes d'absence du salarié connecté (US1). */
@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private readonly api = inject(ApiService);

  getMyAbsences(): Observable<Absence[]> {
    return this.api.get<Absence[]>('/v1/absences/me');
  }

  getMyCounters(): Observable<AbsenceCounter[]> {
    return this.api.get<AbsenceCounter[]>('/v1/absences/me/counters');
  }

  getAbsenceTypes(): Observable<AbsenceType[]> {
    return this.api.get<AbsenceType[]>('/v1/absence-types');
  }

  createAbsence(request: CreateAbsenceRequest): Observable<Absence> {
    return this.api.post<Absence>('/v1/absences', request);
  }

  /** EF-007 : demande de modification d'une absence validée — repasse en validation. */
  modifyAbsence(id: string, request: CreateAbsenceRequest): Observable<Absence> {
    return this.api.put<Absence>(`/v1/absences/${id}/modify`, request);
  }

  cancelAbsence(id: string): Observable<void> {
    return this.api.delete<void>(`/v1/absences/${id}`);
  }
}
