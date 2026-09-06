import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';

export type Role = 'SALARIE' | 'MANAGER' | 'RH' | 'ADMIN';
export type ValidationMode = 'N1_ONLY' | 'N1_AND_N2';
export type WeekDay =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  recurrenceEnabled: boolean;
  roles: Role[];
}

export interface AdminTeam {
  id: string;
  name: string;
  parentTeamId: string | null;
  validationMode: ValidationMode;
  n1ManagerId: string;
  n2ManagerId: string | null;
  active: boolean;
}

export interface AdminAbsenceType {
  id: string;
  code: string;
  label: string;
  color: string | null;
  requiresManagerValidation: boolean;
  requiresJustification: boolean;
  allowedRoles: Role[];
  quantified: boolean;
  annualReset: boolean;
  allowsOverlap: boolean;
  active: boolean;
}

/** T139 — Paramétrage réservé à l'admin technique (EF-031 à EF-034). */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);

  listUsers(): Observable<AdminUser[]> {
    return this.api.get<AdminUser[]>('/v1/admin/users');
  }

  createUser(payload: {
    googleSub: string;
    email: string;
    firstName: string;
    lastName: string;
    roles: Role[];
  }): Observable<AdminUser> {
    return this.api.post<AdminUser>('/v1/admin/users', payload);
  }

  deactivateUser(id: string): Observable<AdminUser> {
    return this.api.put<AdminUser>(`/v1/admin/users/${id}/deactivate`, {});
  }

  assignUserToTeam(userId: string, teamId: string): Observable<void> {
    return this.api.put<void>(`/v1/admin/users/${userId}/team/${teamId}`, {});
  }

  /** EF-031b : planning contractuel du salarié. */
  defineWorkSchedule(
    userId: string,
    payload: { workingDays: WeekDay[]; effectiveFrom: string; effectiveUntil?: string }
  ): Observable<unknown> {
    return this.api.post(`/v1/admin/users/${userId}/work-schedules`, payload);
  }

  listTeams(): Observable<AdminTeam[]> {
    return this.api.get<AdminTeam[]>('/v1/admin/teams');
  }

  createTeam(payload: {
    name: string;
    parentTeamId?: string;
    validationMode: ValidationMode;
    n1ManagerId: string;
    n2ManagerId?: string;
  }): Observable<AdminTeam> {
    return this.api.post<AdminTeam>('/v1/admin/teams', payload);
  }

  listAbsenceTypes(): Observable<AdminAbsenceType[]> {
    return this.api.get<AdminAbsenceType[]>('/v1/admin/absence-types');
  }

  createAbsenceType(payload: Omit<AdminAbsenceType, 'id' | 'active'>): Observable<AdminAbsenceType> {
    return this.api.post<AdminAbsenceType>('/v1/admin/absence-types', payload);
  }

  setAbsenceTypeActive(id: string, active: boolean): Observable<AdminAbsenceType> {
    return this.api.put<AdminAbsenceType>(`/v1/admin/absence-types/${id}/active/${active}`, {});
  }
}
