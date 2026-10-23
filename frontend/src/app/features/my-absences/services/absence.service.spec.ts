import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AbsenceService } from './absence.service';
import { environment } from '../../../../environments/environment';

describe('AbsenceService', () => {
  let service: AbsenceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AbsenceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('appelle GET /v1/absences/me pour la liste des demandes du salarié', () => {
    service.getMyAbsences().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/absences/me`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('appelle GET /v1/absences/me/counters pour les compteurs', () => {
    service.getMyCounters().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/absences/me/counters`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('poste la demande de création sur /v1/absences', () => {
    service
      .createAbsence({
        absenceTypeId: 'type-1',
        startDate: '2026-06-22',
        endDate: '2026-06-23',
        halfDayStart: 'NONE',
        halfDayEnd: 'AM',
      })
      .subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/absences`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.startDate).toBe('2026-06-22');
    expect(req.request.body.halfDayEnd).toBe('AM');
    req.flush({});
  });

  it("supprime la demande via DELETE /v1/absences/{id}", () => {
    service.cancelAbsence('abs-1').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/absences/abs-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
