import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { PlanningEntry, PlanningService } from './services/planning.service';

interface EmployeeRow {
  employeeId: string;
  employeeName: string;
  entries: PlanningEntry[];
}

const MONTH_LABELS = [
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
];

/**
 * T082 — Planning mensuel de l'équipe (EF-015 à EF-017) : une ligne par salarié, une colonne par
 * jour du mois, chaque absence rendue avec la couleur de son type.
 */
@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [CommonModule, ButtonModule, MessageModule, ProgressSpinnerModule, TableModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Planning mensuel</h1>
        <div class="month-nav">
          <p-button
            icon="pi pi-chevron-left"
            [text]="true"
            ariaLabel="Mois précédent"
            (onClick)="shiftMonth(-1)"
          />
          <span class="month-label">{{ monthLabel() }}</span>
          <p-button
            icon="pi pi-chevron-right"
            [text]="true"
            ariaLabel="Mois suivant"
            (onClick)="shiftMonth(1)"
          />
        </div>
        @if (loading()) {
          <p-progressSpinner styleClass="header-spinner" strokeWidth="4" ariaLabel="Chargement" />
        }
      </header>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="grid-scroll">
        <table class="planning-grid">
          <caption class="sr-only">
            Absences de l'équipe pour {{ monthLabel() }}
          </caption>
          <thead>
            <tr>
              <th scope="col" class="employee-col">Salarié</th>
              @for (day of days(); track day) {
                <th scope="col" [class.weekend]="isWeekend(day)">{{ day }}</th>
              }
            </tr>
          </thead>
          <tbody>
            @for (row of rows(); track row.employeeId) {
              <tr>
                <th scope="row" class="employee-col">{{ row.employeeName }}</th>
                @for (day of days(); track day) {
                  <td
                    [class.weekend]="isWeekend(day)"
                    [style.background]="colorFor(row, day)"
                    [attr.title]="labelFor(row, day)"
                    [attr.aria-label]="cellLabel(row, day)"
                  >
                    <span class="sr-only">{{ cellLabel(row, day) }}</span>
                    @if (labelFor(row, day)) {
                      <span aria-hidden="true" class="marker">{{ markerFor(row, day) }}</span>
                    }
                  </td>
                }
              </tr>
            } @empty {
              <tr>
                <td [attr.colspan]="days().length + 1">Aucune absence sur ce mois.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <ul class="legend">
        @for (item of legend(); track item.code) {
          <li>
            <span class="swatch" [style.background]="item.color"></span>{{ item.label }}
          </li>
        }
      </ul>
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
        flex-wrap: wrap;
      }
      h1 {
        font-size: 1.4rem;
        margin: 0;
      }
      .month-nav {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
      .month-label {
        min-width: 10rem;
        text-align: center;
        font-weight: 600;
      }
      .grid-scroll {
        overflow-x: auto;
      }
      .planning-grid {
        border-collapse: collapse;
        font-size: 0.8rem;
      }
      .planning-grid th,
      .planning-grid td {
        border: 1px solid #e0e0e0;
        min-width: 1.6rem;
        height: 1.9rem;
        text-align: center;
      }
      .employee-col {
        min-width: 10rem;
        text-align: left;
        padding: 0 0.5rem;
        position: sticky;
        left: 0;
        background: #ffffff;
      }
      .weekend {
        background: #f2f2f2;
      }
      .legend {
        display: flex;
        gap: 1rem;
        list-style: none;
        margin: 0;
        padding: 0;
        flex-wrap: wrap;
      }
      .legend li {
        display: flex;
        align-items: center;
        gap: 0.35rem;
      }
      .swatch {
        width: 0.9rem;
        height: 0.9rem;
        border-radius: 2px;
        display: inline-block;
      }
      .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0 0 0 0);
        white-space: nowrap;
      }
      .marker {
        font-size: 0.65rem;
        font-weight: 700;
        color: #212121;
      }
      :host ::ng-deep .header-spinner {
        width: 1.5rem;
        height: 1.5rem;
      }
    `,
  ],
})
export class PlanningComponent implements OnInit {
  private readonly planningService = inject(PlanningService);

  readonly entries = signal<PlanningEntry[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** EF-016 : le mois courant est affiché par défaut. */
  readonly month = signal(startOfCurrentMonth());

  readonly monthLabel = computed(
    () => `${MONTH_LABELS[this.month().getMonth()]} ${this.month().getFullYear()}`
  );

  readonly days = computed(() => {
    const date = this.month();
    const count = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
    return Array.from({ length: count }, (_, index) => index + 1);
  });

  readonly rows = computed<EmployeeRow[]>(() => {
    const byEmployee = new Map<string, EmployeeRow>();
    for (const entry of this.entries()) {
      const row = byEmployee.get(entry.employeeId) ?? {
        employeeId: entry.employeeId,
        employeeName: entry.employeeName,
        entries: [],
      };
      row.entries.push(entry);
      byEmployee.set(entry.employeeId, row);
    }
    return [...byEmployee.values()].sort((a, b) => a.employeeName.localeCompare(b.employeeName));
  });

  readonly legend = computed(() => {
    const byCode = new Map<string, { code: string; label: string; color: string }>();
    for (const entry of this.entries()) {
      byCode.set(entry.absenceTypeCode, {
        code: entry.absenceTypeCode,
        label: entry.absenceTypeLabel,
        color: entry.absenceTypeColor ?? '#9e9e9e',
      });
    }
    return [...byCode.values()];
  });

  ngOnInit(): void {
    this.load();
  }

  shiftMonth(offset: number): void {
    const current = this.month();
    this.month.set(new Date(current.getFullYear(), current.getMonth() + offset, 1));
    this.load();
  }

  isWeekend(day: number): boolean {
    const date = new Date(this.month().getFullYear(), this.month().getMonth(), day);
    return date.getDay() === 0 || date.getDay() === 6;
  }

  colorFor(row: EmployeeRow, day: number): string | null {
    const entry = this.entryOn(row, day);
    if (!entry) {
      return null;
    }
    // Une demande encore en attente est rendue en aplat translucide.
    return entry.status === 'VALIDEE'
      ? (entry.absenceTypeColor ?? '#9e9e9e')
      : `color-mix(in srgb, ${entry.absenceTypeColor ?? '#9e9e9e'} 45%, transparent)`;
  }

  labelFor(row: EmployeeRow, day: number): string | null {
    const entry = this.entryOn(row, day);
    return entry ? `${entry.absenceTypeLabel} — ${statusLabel(entry.status)}` : null;
  }

  /** WCAG 1.4.1 : l'absence est aussi restituée en texte, pas seulement par la couleur. */
  cellLabel(row: EmployeeRow, day: number): string {
    const label = this.labelFor(row, day);
    return label
      ? `${row.employeeName}, ${day} ${this.monthLabel()} : ${label}`
      : `${row.employeeName}, ${day} ${this.monthLabel()} : présent`;
  }

  /** Repère visuel non coloré : code court du type d'absence. */
  markerFor(row: EmployeeRow, day: number): string {
    return this.entryOn(row, day)?.absenceTypeCode.slice(0, 2) ?? '';
  }

  private entryOn(row: EmployeeRow, day: number): PlanningEntry | undefined {
    const date = toIsoDate(new Date(this.month().getFullYear(), this.month().getMonth(), day));
    return row.entries.find(entry => entry.startDate <= date && date <= entry.endDate);
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const date = this.month();
    const monthParam = `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
    this.planningService.getMonthlyPlanning(monthParam).subscribe({
      next: entries => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set("Le planning n'a pas pu être chargé.");
      },
    });
  }
}

function startOfCurrentMonth(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1);
}

function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function statusLabel(status: PlanningEntry['status']): string {
  return status === 'VALIDEE' ? 'validée' : 'en attente';
}
