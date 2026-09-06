import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { environment } from '../../../../environments/environment';

export type InjectionSource = 'SAISIE' | 'IMPORT' | 'GENERATION';

export interface InjectionResponse {
  injectionId: string;
  eventsCreatedCount: number;
  absenceTypeCode: string;
  warnings: string[];
}

export interface InjectionLog {
  id: string;
  patternId: string;
  source: InjectionSource;
  injectedAt: string;
  injectedByUserId: string;
  eventsCreatedCount: number;
  periodStart: string;
  periodEnd: string;
}

/** T130 — Module récurrences : saisie, import Excel et historique (EF-028 à EF-030). */
@Injectable({ providedIn: 'root' })
export class RecurrenceService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  /** EF-028 : injection d'une liste de dates pour un patron. */
  generate(
    patternId: string,
    dates: string[],
    source: InjectionSource = 'GENERATION'
  ): Observable<InjectionResponse> {
    return this.api.post<InjectionResponse>('/v1/recurrences/generate', {
      patternId,
      dates,
      source,
    });
  }

  /** EF-028 : import d'un planning école au format Excel. */
  importExcel(patternId: string, file: File): Observable<InjectionResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<InjectionResponse>(
      `${environment.apiUrl}/v1/recurrences/import?patternId=${patternId}`,
      form
    );
  }

  /** EF-029 : historique des injections. */
  getHistory(): Observable<InjectionLog[]> {
    return this.api.get<InjectionLog[]>('/v1/recurrences/history');
  }
}
