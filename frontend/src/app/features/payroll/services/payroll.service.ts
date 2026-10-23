import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { environment } from '../../../../environments/environment';
import {
  Absence,
  AbsenceCounter,
  CreateAbsenceRequest,
} from '../../my-absences/models/absence.model';

export interface PeriodClosure {
  id: string;
  year: number;
  month: number;
  status: 'OPEN' | 'CLOSED';
}

export interface RegularisationEntry {
  id: string;
  periodClosureId: string;
  absenceVersionId: string;
  employeeId: string;
  detectedAt: string;
  description: string | null;
}

export interface HrAbsenceFilters {
  employeeId?: string;
  teamId?: string;
  typeId?: string;
  from?: string;
  to?: string;
}

/** T118 — Accès REST aux fonctions RH et paie (EF-020 à EF-027). */
@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  /** EF-024 : clôture d'une période mensuelle. */
  closePeriod(year: number, month: number): Observable<PeriodClosure> {
    return this.api.post<PeriodClosure>(`/v1/rh/periods/${year}/${month}/close`, {});
  }

  /** EF-026 : régularisations, optionnellement filtrées par période. */
  getRegularisations(year?: number, month?: number): Observable<RegularisationEntry[]> {
    const params: Record<string, string> = {};
    if (year !== undefined && month !== undefined) {
      params['year'] = String(year);
      params['month'] = String(month);
    }
    return this.api.get<RegularisationEntry[]>('/v1/rh/regularisations', params);
  }

  /** EF-021 : consultation de toutes les absences avec filtres cumulables. */
  searchAbsences(filters: HrAbsenceFilters): Observable<Absence[]> {
    const params: Record<string, string> = {};
    for (const [key, value] of Object.entries(filters)) {
      if (value) {
        params[key] = value;
      }
    }
    return this.api.get<Absence[]>('/v1/rh/absences', params);
  }

  /** EF-022 : saisie d'une absence spéciale pour tout salarié. */
  createSpecialAbsence(employeeId: string, request: CreateAbsenceRequest): Observable<Absence> {
    return this.api.post<Absence>(`/v1/rh/absences?employeeId=${employeeId}`, request);
  }

  /** EF-023 : correction d'une absence existante. */
  correctAbsence(absenceId: string, request: CreateAbsenceRequest): Observable<Absence> {
    return this.api.put<Absence>(`/v1/rh/absences/${absenceId}/correct`, request);
  }

  getEmployeeCounters(employeeId: string): Observable<AbsenceCounter[]> {
    return this.api.get<AbsenceCounter[]>(`/v1/rh/employees/${employeeId}/counters`);
  }

  /** EF-027 : export Excel annuel — récupéré en binaire pour être enregistré par le navigateur. */
  downloadAnnualExport(year: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/v1/rh/export/annual/${year}`, {
      responseType: 'blob',
    });
  }
}
