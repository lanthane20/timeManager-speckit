import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AbsenceFormComponent } from './components/absence-form/absence-form.component';
import { AbsenceService } from './services/absence.service';
import { Absence, AbsenceSimplifiedStatus, AbsenceType } from './models/absence.model';

type Severity = 'success' | 'warn' | 'danger' | 'secondary';

/** T060 — Écran « Mes demandes » : liste des demandes du salarié et création d'une demande. */
@Component({
  selector: 'app-my-absences',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    MessageModule,
    TableModule,
    TagModule,
    AbsenceFormComponent,
  ],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Mes demandes d'absence</h1>
        <p-button
          [label]="formVisible() ? 'Fermer le formulaire' : 'Nouvelle demande'"
          [icon]="formVisible() ? 'pi pi-times' : 'pi pi-plus'"
          (onClick)="toggleForm()"
        />
      </header>

      @if (formVisible()) {
        <app-absence-form
          [absenceTypes]="absenceTypes()"
          [absenceToModify]="absenceToModify()"
          (created)="onCreated()"
          (cancelled)="closeForm()"
        />
      }

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table
        [value]="absences()"
        [loading]="loading()"
        responsiveLayout="stack"
        breakpoint="768px"
        dataKey="id"
        [tableStyle]="{ 'min-width': '40rem' }"
      >
        <ng-template pTemplate="header">
          <tr>
            <th scope="col">Type</th>
            <th scope="col">Du</th>
            <th scope="col">Au</th>
            <th scope="col">Jours ouvrés</th>
            <th scope="col">Statut</th>
            <th scope="col">Action</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-absence>
          <tr>
            <td>{{ absence.absenceTypeLabel }}</td>
            <td>{{ absence.startDate | date: 'dd/MM/yyyy' }}</td>
            <td>{{ absence.endDate | date: 'dd/MM/yyyy' }}</td>
            <td>{{ absence.workingDays }}</td>
            <td>
              <p-tag [value]="statusLabel(absence.status)" [severity]="statusSeverity(absence.status)" />
            </td>
            <td class="row-actions">
              @if (isCancellable(absence.status)) {
                <p-button
                  label="Annuler"
                  severity="danger"
                  [text]="true"
                  (onClick)="cancel(absence)"
                />
              }
              @if (isModifiable(absence.status)) {
                <p-button label="Modifier" [text]="true" (onClick)="startModification(absence)" />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="6">Aucune demande d'absence pour le moment.</td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: [
    `
      .page {
        display: grid;
        gap: 1.25rem;
      }
      .page-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        flex-wrap: wrap;
      }
      h1 {
        font-size: 1.4rem;
        margin: 0;
      }
      .row-actions {
        display: flex;
        gap: 0.5rem;
      }
    `,
  ],
})
export class MyAbsencesComponent implements OnInit {
  private readonly absenceService = inject(AbsenceService);

  readonly absences = signal<Absence[]>([]);
  readonly absenceTypes = signal<AbsenceType[]>([]);
  readonly loading = signal(false);
  readonly formVisible = signal(false);
  /** EF-007 : absence validée dont la modification est en cours de saisie. */
  readonly absenceToModify = signal<Absence | null>(null);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadAbsences();
    this.absenceService.getAbsenceTypes().subscribe({
      next: types => this.absenceTypes.set(types),
      error: () => this.errorMessage.set("Les types d'absence n'ont pas pu être chargés."),
    });
  }

  toggleForm(): void {
    this.absenceToModify.set(null);
    this.formVisible.update(visible => !visible);
  }

  closeForm(): void {
    this.absenceToModify.set(null);
    this.formVisible.set(false);
  }

  startModification(absence: Absence): void {
    this.absenceToModify.set(absence);
    this.formVisible.set(true);
  }

  onCreated(): void {
    this.closeForm();
    this.loadAbsences();
  }

  cancel(absence: Absence): void {
    this.absenceService.cancelAbsence(absence.id).subscribe({
      next: () => this.loadAbsences(),
      error: () => this.errorMessage.set("La demande n'a pas pu être annulée."),
    });
  }

  isCancellable(status: AbsenceSimplifiedStatus): boolean {
    return status === 'EN_ATTENTE';
  }

  /** EF-007 : seule une absence validée peut faire l'objet d'une demande de modification. */
  isModifiable(status: AbsenceSimplifiedStatus): boolean {
    return status === 'VALIDEE';
  }

  statusLabel(status: AbsenceSimplifiedStatus): string {
    return STATUS_LABELS[status];
  }

  statusSeverity(status: AbsenceSimplifiedStatus): Severity {
    return STATUS_SEVERITIES[status];
  }

  private loadAbsences(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.absenceService.getMyAbsences().subscribe({
      next: absences => {
        this.absences.set(absences);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Vos demandes n\'ont pas pu être chargées.');
      },
    });
  }
}

const STATUS_LABELS: Record<AbsenceSimplifiedStatus, string> = {
  EN_ATTENTE: 'En attente',
  VALIDEE: 'Validée',
  REFUSEE: 'Refusée',
  ANNULEE: 'Annulée',
};

const STATUS_SEVERITIES: Record<AbsenceSimplifiedStatus, Severity> = {
  EN_ATTENTE: 'warn',
  VALIDEE: 'success',
  REFUSEE: 'danger',
  ANNULEE: 'secondary',
};
