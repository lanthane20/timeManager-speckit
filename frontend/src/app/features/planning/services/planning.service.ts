import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { AbsenceSimplifiedStatus } from '../../my-absences/models/absence.model';

export interface PlanningEntry {
  absenceId: string;
  employeeId: string;
  employeeName: string;
  teamId: string;
  absenceTypeCode: string;
  absenceTypeLabel: string;
  absenceTypeColor: string | null;
  startDate: string;
  endDate: string;
  status: AbsenceSimplifiedStatus;
}

/** T083 — Planning mensuel de l'équipe (EF-015 à EF-017). */
@Injectable({ providedIn: 'root' })
export class PlanningService {
  private readonly api = inject(ApiService);

  /**
   * @param month au format `yyyy-MM` ; omis = mois courant (EF-016)
   * @param teamId équipe ciblée ; omis = tout le périmètre (EF-017)
   */
  getMonthlyPlanning(month?: string, teamId?: string): Observable<PlanningEntry[]> {
    const params: Record<string, string> = {};
    if (month) {
      params['month'] = month;
    }
    if (teamId) {
      params['teamId'] = teamId;
    }
    return this.api.get<PlanningEntry[]>('/v1/planning', params);
  }
}
