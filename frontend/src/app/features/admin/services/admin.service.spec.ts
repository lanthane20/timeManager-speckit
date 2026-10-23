import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { environment } from '../../../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('crée un utilisateur avec ses rôles (EF-031)', () => {
    service
      .createUser({
        googleSub: 'sub-1',
        email: 'nina@timemanager.fr',
        firstName: 'Nina',
        lastName: 'Roche',
        roles: ['SALARIE', 'MANAGER'],
      })
      .subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/admin/users`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.roles).toEqual(['SALARIE', 'MANAGER']);
    req.flush({});
  });

  it('désactive un utilisateur plutôt que de le supprimer (EF-031)', () => {
    service.deactivateUser('user-1').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/admin/users/user-1/deactivate`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('enregistre le planning contractuel avec sa date d\'effet (EF-031b)', () => {
    service
      .defineWorkSchedule('user-1', {
        workingDays: ['MONDAY', 'TUESDAY', 'THURSDAY', 'FRIDAY'],
        effectiveFrom: '2026-01-01',
      })
      .subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/admin/users/user-1/work-schedules`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.workingDays).not.toContain('WEDNESDAY');
    expect(req.request.body.effectiveFrom).toBe('2026-01-01');
    req.flush({});
  });

  it("bascule le statut actif d'un type d'absence (EF-034)", () => {
    service.setAbsenceTypeActive('type-1', false).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/admin/absence-types/type-1/active/false`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });
});
