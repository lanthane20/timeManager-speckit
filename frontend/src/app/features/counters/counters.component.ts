import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { AbsenceService } from '../my-absences/services/absence.service';
import { AbsenceCounter, AbsenceType } from '../my-absences/models/absence.model';

interface CounterRow {
  label: string;
  daysTakenPast: number;
  daysValidatedFuture: number;
  daysPending: number;
}

/**
 * T062 — Compteurs d'absence du salarié (EF-018). Conformément à EF-019, aucun solde ni droit acquis
 * n'est affiché : uniquement les jours consommés, validés à venir et en attente.
 */
@Component({
  selector: 'app-counters',
  standalone: true,
  imports: [CommonModule, CardModule, MessageModule, TableModule],
  template: `
    <p-card header="Mes compteurs d'absence">
      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table
        [value]="rows()"
        [loading]="loading()"
        responsiveLayout="stack"
        breakpoint="768px"
        [tableStyle]="{ 'min-width': '30rem' }"
      >
        <ng-template pTemplate="header">
          <tr>
            <th scope="col">Type d'absence</th>
            <th scope="col">Jours pris</th>
            <th scope="col">Validés à venir</th>
            <th scope="col">En attente</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-row>
          <tr>
            <td>{{ row.label }}</td>
            <td>{{ row.daysTakenPast }}</td>
            <td>{{ row.daysValidatedFuture }}</td>
            <td>{{ row.daysPending }}</td>
          </tr>
        </ng-template>
        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="4">Aucune consommation enregistrée sur l'année en cours.</td>
          </tr>
        </ng-template>
      </p-table>
    </p-card>
  `,
})
export class CountersComponent implements OnInit {
  private readonly absenceService = inject(AbsenceService);

  readonly rows = signal<CounterRow[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.absenceService.getAbsenceTypes().subscribe({
      next: types => this.loadCounters(types),
      error: () => this.fail(),
    });
  }

  private loadCounters(types: AbsenceType[]): void {
    const labelsById = new Map(types.map(type => [type.id, type.label]));
    this.absenceService.getMyCounters().subscribe({
      next: counters => {
        this.rows.set(counters.map(counter => toRow(counter, labelsById)));
        this.loading.set(false);
      },
      error: () => this.fail(),
    });
  }

  private fail(): void {
    this.loading.set(false);
    this.errorMessage.set("Vos compteurs n'ont pas pu être chargés.");
  }
}

function toRow(counter: AbsenceCounter, labelsById: Map<string, string>): CounterRow {
  return {
    label: labelsById.get(counter.absenceTypeId) ?? 'Type inconnu',
    daysTakenPast: counter.daysTakenPast,
    daysValidatedFuture: counter.daysValidatedFuture,
    daysPending: counter.daysPending,
  };
}
