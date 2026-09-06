import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { PayrollService, RegularisationEntry } from './services/payroll.service';
import { AbsenceService } from '../my-absences/services/absence.service';
import {
  Absence,
  AbsenceType,
  CreateAbsenceRequest,
  HalfDayType,
} from '../my-absences/models/absence.model';

/**
 * T118 — Tableau de bord RH (EF-020 à EF-027) : clôture mensuelle, régularisations, export annuel,
 * consultation filtrée des absences, saisie d'absence spéciale et correction.
 */
@Component({
  selector: 'app-payroll',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    DatePickerModule,
    InputNumberModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    SelectModule,
    TableModule,
    TabsModule,
    TagModule,
  ],
  templateUrl: './payroll.component.html',
  styleUrl: './payroll.component.scss',
})
export class PayrollComponent implements OnInit {
  private readonly payrollService = inject(PayrollService);
  private readonly absenceService = inject(AbsenceService);
  private readonly fb = inject(FormBuilder);

  readonly absenceTypes = signal<AbsenceType[]>([]);
  readonly regularisations = signal<RegularisationEntry[]>([]);
  readonly absences = signal<Absence[]>([]);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  /** Absence sélectionnée pour correction (EF-023) ; absent = saisie d'une absence spéciale. */
  readonly absenceToCorrect = signal<Absence | null>(null);

  readonly monthOptions = MONTH_OPTIONS;

  readonly closureForm = this.fb.nonNullable.group({
    year: [new Date().getFullYear(), Validators.required],
    month: [new Date().getMonth() + 1, Validators.required],
  });

  readonly filterForm = this.fb.nonNullable.group({
    employeeId: [''],
    teamId: [''],
    typeId: [''],
    from: [null as Date | null],
    to: [null as Date | null],
  });

  readonly absenceForm = this.fb.nonNullable.group({
    employeeId: ['', Validators.required],
    absenceTypeId: ['', Validators.required],
    startDate: [null as Date | null, Validators.required],
    endDate: [null as Date | null, Validators.required],
    halfDayStart: ['NONE' as HalfDayType],
    halfDayEnd: ['NONE' as HalfDayType],
  });

  readonly halfDayOptions = [
    { label: 'Journée entière', value: 'NONE' as HalfDayType },
    { label: 'Matin', value: 'AM' as HalfDayType },
    { label: 'Après-midi', value: 'PM' as HalfDayType },
  ];

  readonly exportYear = signal(new Date().getFullYear());

  ngOnInit(): void {
    this.absenceService.getAbsenceTypes().subscribe({
      next: types => this.absenceTypes.set(types),
      error: () => this.fail("Les types d'absence n'ont pas pu être chargés."),
    });
    this.loadRegularisations();
  }

  /** EF-024 : clôture de la période sélectionnée. */
  closePeriod(): void {
    const { year, month } = this.closureForm.getRawValue();
    this.start();
    this.payrollService.closePeriod(year, month).subscribe({
      next: closure => {
        this.done(`Période ${closure.month}/${closure.year} clôturée.`);
        this.loadRegularisations();
      },
      error: (error: { status?: number }) =>
        this.fail(
          error.status === 409
            ? 'Cette période est déjà clôturée.'
            : "La période n'a pas pu être clôturée."
        ),
    });
  }

  /** EF-026 : régularisations de la période saisie, ou toutes si aucune période. */
  loadRegularisations(filtered = false): void {
    const { year, month } = this.closureForm.getRawValue();
    this.start();
    this.payrollService.getRegularisations(...(filtered ? [year, month] : [])).subscribe({
      next: entries => {
        this.regularisations.set(entries);
        this.done(null);
      },
      error: () => this.fail("Les régularisations n'ont pas pu être chargées."),
    });
  }

  /** EF-021 : recherche filtrée. */
  searchAbsences(): void {
    const value = this.filterForm.getRawValue();
    this.start();
    this.payrollService
      .searchAbsences({
        employeeId: value.employeeId || undefined,
        teamId: value.teamId || undefined,
        typeId: value.typeId || undefined,
        from: value.from ? toIsoDate(value.from) : undefined,
        to: value.to ? toIsoDate(value.to) : undefined,
      })
      .subscribe({
        next: absences => {
          this.absences.set(absences);
          this.done(null);
        },
        error: () => this.fail("La recherche n'a pas abouti."),
      });
  }

  /** EF-023 : pré-remplit le formulaire avec l'absence à corriger. */
  startCorrection(absence: Absence): void {
    this.absenceToCorrect.set(absence);
    this.absenceForm.patchValue({
      employeeId: absence.employeeId,
      absenceTypeId: absence.absenceTypeId,
      startDate: new Date(absence.startDate),
      endDate: new Date(absence.endDate),
    });
  }

  cancelCorrection(): void {
    this.absenceToCorrect.set(null);
    this.absenceForm.reset({ halfDayStart: 'NONE', halfDayEnd: 'NONE' });
  }

  /** EF-022 (saisie) ou EF-023 (correction) selon le mode courant. */
  submitAbsence(): void {
    if (this.absenceForm.invalid) {
      return;
    }
    const value = this.absenceForm.getRawValue();
    const request: CreateAbsenceRequest = {
      absenceTypeId: value.absenceTypeId,
      startDate: toIsoDate(value.startDate!),
      endDate: toIsoDate(value.endDate!),
      halfDayStart: value.halfDayStart,
      halfDayEnd: value.halfDayEnd,
    };
    const correction = this.absenceToCorrect();
    this.start();
    const call = correction
      ? this.payrollService.correctAbsence(correction.id, request)
      : this.payrollService.createSpecialAbsence(value.employeeId, request);
    call.subscribe({
      next: () => {
        this.done(correction ? 'Absence corrigée.' : 'Absence spéciale enregistrée.');
        this.cancelCorrection();
        this.searchAbsences();
      },
      error: () => this.fail("L'absence n'a pas pu être enregistrée."),
    });
  }

  /** EF-027 : téléchargement de l'export annuel. */
  downloadExport(): void {
    const year = this.exportYear();
    this.start();
    this.payrollService.downloadAnnualExport(year).subscribe({
      next: blob => {
        saveBlob(blob, `absences-${year}.xlsx`);
        this.done('Export généré.');
      },
      error: () => this.fail("L'export n'a pas pu être généré."),
    });
  }

  private start(): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private done(message: string | null): void {
    this.busy.set(false);
    this.successMessage.set(message);
  }

  private fail(message: string): void {
    this.busy.set(false);
    this.errorMessage.set(message);
  }
}

const MONTH_OPTIONS = [
  'janvier',
  'février',
  'mars',
  'avril',
  'mai',
  'juin',
  'juillet',
  'août',
  'septembre',
  'octobre',
  'novembre',
  'décembre',
].map((label, index) => ({ label, value: index + 1 }));

function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
