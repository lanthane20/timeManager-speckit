import { Component, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { AbsenceService } from '../../services/absence.service';
import { Absence, AbsenceType, CreateAbsenceRequest, HalfDayType } from '../../models/absence.model';

interface HalfDayOption {
  label: string;
  value: HalfDayType;
}

/** T061 — Formulaire de création d'une demande d'absence (type, dates, demi-journées). */
@Component({
  selector: 'app-absence-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    DatePickerModule,
    MessageModule,
    SelectModule,
  ],
  template: `
    <form class="absence-form" [formGroup]="form" (ngSubmit)="submit()">
      <div class="field">
        <label for="absenceType">Type d'absence</label>
        <p-select
          inputId="absenceType"
          formControlName="absenceTypeId"
          [options]="absenceTypes()"
          optionLabel="label"
          optionValue="id"
          placeholder="Sélectionner un type"
          appendTo="body"
        />
      </div>

      <div class="field">
        <label for="startDate">Date de début</label>
        <p-datepicker
          inputId="startDate"
          formControlName="startDate"
          dateFormat="dd/mm/yy"
          [showIcon]="true"
          appendTo="body"
        />
      </div>

      <div class="field">
        <label for="halfDayStart">Début</label>
        <p-select
          inputId="halfDayStart"
          formControlName="halfDayStart"
          [options]="halfDayOptions"
          optionLabel="label"
          optionValue="value"
          appendTo="body"
        />
      </div>

      <div class="field">
        <label for="endDate">Date de fin</label>
        <p-datepicker
          inputId="endDate"
          formControlName="endDate"
          dateFormat="dd/mm/yy"
          [showIcon]="true"
          appendTo="body"
        />
      </div>

      <div class="field">
        <label for="halfDayEnd">Fin</label>
        <p-select
          inputId="halfDayEnd"
          formControlName="halfDayEnd"
          [options]="halfDayOptions"
          optionLabel="label"
          optionValue="value"
          appendTo="body"
        />
      </div>

      @if (absenceToModify()) {
        <p-message
          severity="info"
          text="L'absence actuelle reste valide jusqu'à l'approbation de cette modification."
        />
      }

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="actions">
        <p-button
          type="submit"
          [label]="absenceToModify() ? 'Envoyer la modification' : 'Envoyer la demande'"
          [disabled]="form.invalid || submitting()"
          [loading]="submitting()"
        />
        <p-button
          type="button"
          label="Annuler"
          severity="secondary"
          [text]="true"
          (onClick)="cancelled.emit()"
        />
      </div>
    </form>
  `,
  styles: [
    `
      .absence-form {
        display: grid;
        gap: 1rem;
        max-width: 640px;
      }
      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }
      label {
        font-weight: 600;
      }
      .actions {
        display: flex;
        gap: 0.75rem;
      }
      @media (min-width: 768px) {
        .absence-form {
          grid-template-columns: 1fr 1fr;
        }
        .actions,
        p-message {
          grid-column: 1 / -1;
        }
      }
    `,
  ],
})
export class AbsenceFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly absenceService = inject(AbsenceService);

  readonly absenceTypes = input.required<AbsenceType[]>();
  /** Absence validée en cours de modification (EF-007) ; absent = création d'une demande. */
  readonly absenceToModify = input<Absence | null>(null);
  readonly created = output<void>();
  readonly cancelled = output<void>();

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly halfDayOptions: HalfDayOption[] = [
    { label: 'Journée entière', value: 'NONE' },
    { label: 'Matin', value: 'AM' },
    { label: 'Après-midi', value: 'PM' },
  ];

  readonly form = this.fb.nonNullable.group({
    absenceTypeId: ['', Validators.required],
    startDate: [null as Date | null, Validators.required],
    endDate: [null as Date | null, Validators.required],
    halfDayStart: ['NONE' as HalfDayType],
    halfDayEnd: ['NONE' as HalfDayType],
  });

  constructor() {
    effect(() => {
      const absence = this.absenceToModify();
      if (absence) {
        this.form.patchValue({
          absenceTypeId: absence.absenceTypeId,
          startDate: new Date(absence.startDate),
          endDate: new Date(absence.endDate),
        });
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    const value = this.form.getRawValue();
    const request: CreateAbsenceRequest = {
      absenceTypeId: value.absenceTypeId,
      startDate: toIsoDate(value.startDate!),
      endDate: toIsoDate(value.endDate!),
      halfDayStart: value.halfDayStart,
      halfDayEnd: value.halfDayEnd,
    };

    this.submitting.set(true);
    this.errorMessage.set(null);
    const absence = this.absenceToModify();
    const call = absence
      ? this.absenceService.modifyAbsence(absence.id, request)
      : this.absenceService.createAbsence(request);
    call.subscribe({
      next: () => {
        this.submitting.set(false);
        this.form.reset({ halfDayStart: 'NONE', halfDayEnd: 'NONE', absenceTypeId: '' });
        this.created.emit();
      },
      error: (error: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        this.errorMessage.set(describeError(error));
      },
    });
  }
}

/** Sérialise en date locale ISO (yyyy-MM-dd) sans décalage de fuseau. */
function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function describeError(error: { status?: number; error?: { message?: string } }): string {
  if (error.status === 409) {
    return "Cette demande chevauche une absence existante.";
  }
  if (error.status === 403) {
    return "Vous n'êtes pas autorisé à créer cette demande.";
  }
  return error.error?.message ?? "La demande n'a pas pu être enregistrée.";
}
