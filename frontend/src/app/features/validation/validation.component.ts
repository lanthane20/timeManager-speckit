import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ManagerAbsenceService } from './services/manager-absence.service';
import { Absence } from '../my-absences/models/absence.model';

/**
 * T081, T094 — Files de validation du manager (EF-009 à EF-012). Le premier niveau (N+1) et le
 * second niveau (N+2) sont présentés séparément. T085 : chaque action affiche un retour visible
 * immédiat (bouton en chargement) pour rester sous 200 ms perçues.
 */
@Component({
  selector: 'app-validation',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    DialogModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TabsModule,
    TagModule,
  ],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Demandes à valider</h1>
        @if (loading()) {
          <p-progressSpinner styleClass="header-spinner" strokeWidth="4" ariaLabel="Chargement" />
        }
      </header>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-tabs value="n1">
        <p-tablist>
          <p-tab value="n1">Ma validation ({{ firstLevel().length }})</p-tab>
          @if (secondLevel().length > 0) {
            <p-tab value="n2">Validation N+2 ({{ secondLevel().length }})</p-tab>
          }
        </p-tablist>
        <p-tabpanels>
          <p-tabpanel value="n1">
            <ng-container
              *ngTemplateOutlet="queue; context: { $implicit: firstLevel(), level: 'n1' }"
            />
          </p-tabpanel>
          <p-tabpanel value="n2">
            <ng-container
              *ngTemplateOutlet="queue; context: { $implicit: secondLevel(), level: 'n2' }"
            />
          </p-tabpanel>
        </p-tabpanels>
      </p-tabs>

      <ng-template #queue let-absences let-level="level">
        <p-table
          [value]="absences"
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
              <th scope="col">Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-absence>
            <tr>
              <td>{{ absence.absenceTypeLabel }}</td>
              <td>{{ absence.startDate | date: 'dd/MM/yyyy' }}</td>
              <td>{{ absence.endDate | date: 'dd/MM/yyyy' }}</td>
              <td>{{ absence.workingDays }}</td>
              <td>
                <p-tag
                  [value]="level === 'n2' ? 'Validée N+1' : 'En attente'"
                  [severity]="level === 'n2' ? 'info' : 'warn'"
                />
              </td>
              <td class="actions">
                <p-button
                  label="Valider"
                  severity="success"
                  [loading]="pendingActionId() === absence.id"
                  [disabled]="pendingActionId() !== null"
                  (onClick)="validate(absence, level)"
                />
                <p-button
                  label="Refuser"
                  severity="danger"
                  [text]="true"
                  [disabled]="pendingActionId() !== null"
                  (onClick)="openRefusal(absence)"
                />
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="6">Aucune demande en attente à ce niveau de validation.</td>
            </tr>
          </ng-template>
        </p-table>
      </ng-template>

      <p-dialog
        header="Refuser la demande"
        [(visible)]="refusalDialogVisible"
        [modal]="true"
        [style]="{ width: '32rem' }"
      >
        <label for="refusalReason">Motif du refus (obligatoire)</label>
        <textarea
          id="refusalReason"
          rows="4"
          class="refusal-reason"
          [(ngModel)]="refusalReason"
          [attr.aria-invalid]="refusalReason.trim().length === 0"
        ></textarea>
        @if (refusalReason.trim().length === 0) {
          <p-message severity="warn" text="Un refus sans motif est impossible." />
        }
        <ng-template pTemplate="footer">
          <p-button label="Annuler" severity="secondary" [text]="true" (onClick)="closeRefusal()" />
          <p-button
            label="Confirmer le refus"
            severity="danger"
            [disabled]="refusalReason.trim().length === 0"
            [loading]="pendingActionId() !== null"
            (onClick)="confirmRefusal()"
          />
        </ng-template>
      </p-dialog>
    </section>
  `,
  styles: [
    `
      .page {
        display: grid;
        /* minmax(0, 1fr) : un tableau large défile dans sa carte au lieu d'élargir la page. */
        grid-template-columns: minmax(0, 1fr);
        gap: 1.25rem;
      }
      .page-header {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      h1 {
        font-size: 1.4rem;
        margin: 0;
      }
      .actions {
        display: flex;
        gap: 0.5rem;
      }
      .refusal-reason {
        width: 100%;
        margin: 0.35rem 0 0.75rem;
      }
      :host ::ng-deep .header-spinner {
        width: 1.5rem;
        height: 1.5rem;
      }
    `,
  ],
})
export class ValidationComponent implements OnInit {
  private readonly managerAbsenceService = inject(ManagerAbsenceService);

  readonly firstLevel = signal<Absence[]>([]);
  readonly secondLevel = signal<Absence[]>([]);
  readonly loading = signal(false);
  readonly pendingActionId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  refusalDialogVisible = false;
  refusalReason = '';
  private absenceToRefuse: Absence | null = null;

  ngOnInit(): void {
    this.load();
  }

  validate(absence: Absence, level: 'n1' | 'n2'): void {
    this.pendingActionId.set(absence.id);
    this.errorMessage.set(null);
    const request =
      level === 'n2'
        ? this.managerAbsenceService.validateN2(absence.id)
        : this.managerAbsenceService.validate(absence.id);
    request.subscribe({
      next: () => {
        this.pendingActionId.set(null);
        this.load();
      },
      error: () => this.fail("La demande n'a pas pu être validée."),
    });
  }

  openRefusal(absence: Absence): void {
    this.absenceToRefuse = absence;
    this.refusalReason = '';
    this.refusalDialogVisible = true;
  }

  closeRefusal(): void {
    this.refusalDialogVisible = false;
    this.absenceToRefuse = null;
  }

  confirmRefusal(): void {
    const absence = this.absenceToRefuse;
    const reason = this.refusalReason.trim();
    if (!absence || reason.length === 0) {
      return;
    }
    this.pendingActionId.set(absence.id);
    this.errorMessage.set(null);
    this.managerAbsenceService.refuse(absence.id, reason).subscribe({
      next: () => {
        this.pendingActionId.set(null);
        this.closeRefusal();
        this.load();
      },
      error: () => this.fail("Le refus n'a pas pu être enregistré."),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.managerAbsenceService.getPendingAbsences().subscribe({
      next: absences => {
        this.firstLevel.set(absences);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set("La file de validation n'a pas pu être chargée.");
      },
    });
    // La file N+2 n'existe que pour les managers N+2 d'équipes en double validation :
    // une absence de droits se traduit par une file vide, pas par une erreur affichée.
    this.managerAbsenceService.getPendingSecondLevelAbsences().subscribe({
      next: absences => this.secondLevel.set(absences),
      error: () => this.secondLevel.set([]),
    });
  }

  private fail(message: string): void {
    this.pendingActionId.set(null);
    this.errorMessage.set(message);
  }
}
