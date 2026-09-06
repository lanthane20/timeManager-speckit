import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ManagerAbsenceService } from './manager-absence.service';
import { PlanningService } from '../../planning/services/planning.service';
import { environment } from '../../../../environments/environment';

describe('Services manager', () => {
  let managerService: ManagerAbsenceService;
  let planningService: PlanningService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    managerService = TestBed.inject(ManagerAbsenceService);
    planningService = TestBed.inject(PlanningService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('charge la file de validation du manager', () => {
    managerService.getPendingAbsences().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/manager/absences/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('valide une demande via PUT', () => {
    managerService.validate('abs-1').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/manager/absences/abs-1/validate`);
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });

  it('transmet le motif obligatoire lors du refus (EF-011)', () => {
    managerService.refuse('abs-1', 'Effectif insuffisant').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/manager/absences/abs-1/refuse`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.reason).toBe('Effectif insuffisant');
    req.flush(null);
  });

  it('charge la file de second niveau du N+2 (EF-012)', () => {
    managerService.getPendingSecondLevelAbsences().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/manager/absences/pending-n2`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('valide au second niveau via un endpoint distinct (EF-012)', () => {
    managerService.validateN2('abs-1').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/manager/absences/abs-1/validate-n2`);
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });

  it('appelle le planning sans paramètre pour le mois courant (EF-016)', () => {
    planningService.getMonthlyPlanning().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/planning`);
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('transmet les filtres mois et équipe au planning (EF-017)', () => {
    planningService.getMonthlyPlanning('2026-09', 'team-1').subscribe();
    const req = httpMock.expectOne(
      request =>
        request.url === `${environment.apiUrl}/v1/planning` &&
        request.params.get('month') === '2026-09' &&
        request.params.get('teamId') === 'team-1'
    );
    req.flush([]);
  });
});
