import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PayrollService } from './payroll.service';
import { environment } from '../../../../environments/environment';

describe('PayrollService', () => {
  let service: PayrollService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PayrollService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('clôture une période mensuelle (EF-024)', () => {
    service.closePeriod(2026, 9).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/rh/periods/2026/9/close`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'p1', year: 2026, month: 9, status: 'CLOSED' });
  });

  it('charge toutes les régularisations quand aucune période n\'est précisée (EF-026)', () => {
    service.getRegularisations().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/rh/regularisations`);
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('filtre les régularisations par période (EF-026)', () => {
    service.getRegularisations(2026, 9).subscribe();
    const req = httpMock.expectOne(
      request =>
        request.url === `${environment.apiUrl}/v1/rh/regularisations` &&
        request.params.get('year') === '2026' &&
        request.params.get('month') === '9'
    );
    req.flush([]);
  });

  it('omet les filtres vides lors de la recherche RH (EF-021)', () => {
    service.searchAbsences({ employeeId: 'emp-1', teamId: '', from: '2026-01-01' }).subscribe();
    const req = httpMock.expectOne(
      request =>
        request.url === `${environment.apiUrl}/v1/rh/absences` &&
        request.params.get('employeeId') === 'emp-1' &&
        request.params.get('from') === '2026-01-01' &&
        !request.params.has('teamId')
    );
    req.flush([]);
  });

  it('récupère l\'export annuel en binaire (EF-027)', () => {
    service.downloadAnnualExport(2026).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/rh/export/annual/2026`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
