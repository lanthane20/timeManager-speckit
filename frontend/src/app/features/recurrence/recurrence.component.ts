import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { InjectionLog, InjectionResponse, RecurrenceService } from './services/recurrence.service';

const SOURCE_LABELS: Record<InjectionLog['source'], string> = {
  SAISIE: 'Saisie',
  IMPORT: 'Import Excel',
  GENERATION: 'Génération',
};

/**
 * T130 — Module récurrences (EF-028 à EF-030) : saisie de dates, import d'un planning Excel et
 * historique des injections. Les chevauchements sont affichés comme avertissements non bloquants.
 */
@Component({
  selector: 'app-recurrence',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    DatePickerModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './recurrence.component.html',
  styleUrl: './recurrence.component.scss',
})
export class RecurrenceComponent implements OnInit {
  private readonly recurrenceService = inject(RecurrenceService);
  private readonly fb = inject(FormBuilder);

  readonly history = signal<InjectionLog[]>([]);
  readonly warnings = signal<string[]>([]);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly selectedFileName = signal<string | null>(null);

  private selectedFile: File | null = null;

  readonly form = this.fb.nonNullable.group({
    patternId: ['', Validators.required],
    dates: [[] as Date[]],
  });

  ngOnInit(): void {
    this.loadHistory();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.selectedFileName.set(this.selectedFile?.name ?? null);
  }

  /** EF-028 : injection des dates saisies dans le calendrier. */
  generate(): void {
    const { patternId, dates } = this.form.getRawValue();
    if (!patternId || dates.length === 0) {
      this.errorMessage.set('Renseignez un patron et au moins une date.');
      return;
    }
    this.start();
    this.recurrenceService.generate(patternId, dates.map(toIsoDate), 'SAISIE').subscribe({
      next: response => this.onInjected(response),
      error: (error: { status?: number }) => this.onInjectionError(error),
    });
  }

  /** EF-028 : import d'un planning école. */
  importExcel(): void {
    const patternId = this.form.getRawValue().patternId;
    if (!patternId || !this.selectedFile) {
      this.errorMessage.set('Renseignez un patron et sélectionnez un fichier Excel.');
      return;
    }
    this.start();
    this.recurrenceService.importExcel(patternId, this.selectedFile).subscribe({
      next: response => this.onInjected(response),
      error: (error: { status?: number }) => this.onInjectionError(error),
    });
  }

  sourceLabel(source: InjectionLog['source']): string {
    return SOURCE_LABELS[source];
  }

  private loadHistory(): void {
    this.recurrenceService.getHistory().subscribe({
      next: logs => this.history.set(logs),
      error: (error: { status?: number }) =>
        this.errorMessage.set(
          error.status === 403
            ? "Le module récurrences n'est pas activé pour votre compte."
            : "L'historique n'a pas pu être chargé."
        ),
    });
  }

  private onInjected(response: InjectionResponse): void {
    this.busy.set(false);
    this.warnings.set(response.warnings);
    this.successMessage.set(
      `${response.eventsCreatedCount} absence(s) « ${response.absenceTypeCode} » injectée(s).`
    );
    this.loadHistory();
  }

  private onInjectionError(error: { status?: number }): void {
    this.busy.set(false);
    this.errorMessage.set(
      error.status === 403
        ? "Le module récurrences n'est pas activé pour votre compte."
        : "L'injection n'a pas pu être réalisée."
    );
  }

  private start(): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.warnings.set([]);
  }
}

function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
