import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RecurrenceService } from './recurrence.service';
import { environment } from '../../../../environments/environment';

describe('RecurrenceService', () => {
  let service: RecurrenceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RecurrenceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('injecte les dates saisies avec la source correspondante (EF-028)', () => {
    service.generate('pattern-1', ['2026-09-07', '2026-09-14'], 'SAISIE').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/recurrences/generate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.patternId).toBe('pattern-1');
    expect(req.request.body.dates).toEqual(['2026-09-07', '2026-09-14']);
    expect(req.request.body.source).toBe('SAISIE');
    req.flush({ injectionId: 'i1', eventsCreatedCount: 2, absenceTypeCode: 'ECOLE', warnings: [] });
  });

  it('envoie le fichier Excel en multipart avec le patron ciblé (EF-028)', () => {
    const file = new File(['contenu'], 'planning.xlsx');
    service.importExcel('pattern-1', file).subscribe();
    const req = httpMock.expectOne(
      `${environment.apiUrl}/v1/recurrences/import?patternId=pattern-1`
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush({ injectionId: 'i2', eventsCreatedCount: 12, absenceTypeCode: 'ECOLE', warnings: [] });
  });

  it("charge l'historique des injections (EF-029)", () => {
    service.getHistory().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/v1/recurrences/history`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
