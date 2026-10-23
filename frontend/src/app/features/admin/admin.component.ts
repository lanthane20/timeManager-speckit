import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MultiSelectModule } from 'primeng/multiselect';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import {
  AdminAbsenceType,
  AdminService,
  AdminTeam,
  AdminUser,
  Role,
  ValidationMode,
  WeekDay,
} from './services/admin.service';

const ROLE_OPTIONS: { label: string; value: Role }[] = [
  { label: 'Salarié', value: 'SALARIE' },
  { label: 'Manager', value: 'MANAGER' },
  { label: 'RH', value: 'RH' },
  { label: 'Admin technique', value: 'ADMIN' },
];

const DAY_OPTIONS: { label: string; value: WeekDay }[] = [
  { label: 'Lundi', value: 'MONDAY' },
  { label: 'Mardi', value: 'TUESDAY' },
  { label: 'Mercredi', value: 'WEDNESDAY' },
  { label: 'Jeudi', value: 'THURSDAY' },
  { label: 'Vendredi', value: 'FRIDAY' },
];

const VALIDATION_MODE_OPTIONS: { label: string; value: ValidationMode }[] = [
  { label: 'N+1 seul', value: 'N1_ONLY' },
  { label: 'N+1 puis N+2', value: 'N1_AND_N2' },
];

/**
 * T139 — Administration technique (EF-031 à EF-035) : utilisateurs, planning contractuel, équipes et
 * types d'absence. Aucun écran de ce module n'expose de donnée d'absence individuelle (EF-035).
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    CheckboxModule,
    DatePickerModule,
    InputTextModule,
    MessageModule,
    MultiSelectModule,
    ProgressSpinnerModule,
    SelectModule,
    TableModule,
    TabsModule,
    TagModule,
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  readonly users = signal<AdminUser[]>([]);
  readonly teams = signal<AdminTeam[]>([]);
  readonly absenceTypes = signal<AdminAbsenceType[]>([]);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly roleOptions = ROLE_OPTIONS;
  readonly dayOptions = DAY_OPTIONS;
  readonly validationModeOptions = VALIDATION_MODE_OPTIONS;

  readonly userForm = this.fb.nonNullable.group({
    googleSub: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    roles: [['SALARIE'] as Role[], Validators.required],
  });

  readonly scheduleForm = this.fb.nonNullable.group({
    userId: ['', Validators.required],
    workingDays: [
      ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'] as WeekDay[],
      Validators.required,
    ],
    effectiveFrom: [null as Date | null, Validators.required],
    effectiveUntil: [null as Date | null],
  });

  readonly teamForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    validationMode: ['N1_ONLY' as ValidationMode, Validators.required],
    n1ManagerId: ['', Validators.required],
    n2ManagerId: [''],
  });

  readonly absenceTypeForm = this.fb.nonNullable.group({
    code: ['', Validators.required],
    label: ['', Validators.required],
    color: ['#3F51B5'],
    requiresManagerValidation: [true],
    requiresJustification: [false],
    allowedRoles: [['SALARIE'] as Role[]],
    quantified: [true],
    annualReset: [true],
    allowsOverlap: [false],
  });

  ngOnInit(): void {
    this.reloadAll();
  }

  createUser(): void {
    if (this.userForm.invalid) {
      return;
    }
    this.start();
    this.adminService.createUser(this.userForm.getRawValue()).subscribe({
      next: () => {
        this.done('Utilisateur créé.');
        this.userForm.reset({ roles: ['SALARIE'] });
        this.loadUsers();
      },
      error: () => this.fail("L'utilisateur n'a pas pu être créé."),
    });
  }

  deactivateUser(user: AdminUser): void {
    this.start();
    this.adminService.deactivateUser(user.id).subscribe({
      next: () => {
        this.done('Utilisateur désactivé.');
        this.loadUsers();
      },
      error: () => this.fail("L'utilisateur n'a pas pu être désactivé."),
    });
  }

  /** EF-031b : planning contractuel avec date d'effet. */
  defineWorkSchedule(): void {
    if (this.scheduleForm.invalid) {
      return;
    }
    const value = this.scheduleForm.getRawValue();
    this.start();
    this.adminService
      .defineWorkSchedule(value.userId, {
        workingDays: value.workingDays,
        effectiveFrom: toIsoDate(value.effectiveFrom!),
        effectiveUntil: value.effectiveUntil ? toIsoDate(value.effectiveUntil) : undefined,
      })
      .subscribe({
        next: () => this.done('Planning contractuel enregistré.'),
        error: () => this.fail("Le planning n'a pas pu être enregistré."),
      });
  }

  createTeam(): void {
    if (this.teamForm.invalid) {
      return;
    }
    const value = this.teamForm.getRawValue();
    this.start();
    this.adminService
      .createTeam({
        name: value.name,
        validationMode: value.validationMode,
        n1ManagerId: value.n1ManagerId,
        n2ManagerId: value.n2ManagerId || undefined,
      })
      .subscribe({
        next: () => {
          this.done('Équipe créée.');
          this.teamForm.reset({ validationMode: 'N1_ONLY' });
          this.loadTeams();
        },
        error: (error: { error?: { message?: string } }) =>
          this.fail(error.error?.message ?? "L'équipe n'a pas pu être créée."),
      });
  }

  createAbsenceType(): void {
    if (this.absenceTypeForm.invalid) {
      return;
    }
    this.start();
    this.adminService.createAbsenceType(this.absenceTypeForm.getRawValue()).subscribe({
      next: () => {
        this.done("Type d'absence créé et disponible à la saisie.");
        this.absenceTypeForm.reset({
          color: '#3F51B5',
          requiresManagerValidation: true,
          allowedRoles: ['SALARIE'],
          quantified: true,
          annualReset: true,
        });
        this.loadAbsenceTypes();
      },
      error: (error: { error?: { message?: string } }) =>
        this.fail(error.error?.message ?? "Le type d'absence n'a pas pu être créé."),
    });
  }

  toggleAbsenceType(type: AdminAbsenceType): void {
    this.start();
    this.adminService.setAbsenceTypeActive(type.id, !type.active).subscribe({
      next: () => {
        this.done(type.active ? 'Type désactivé.' : 'Type réactivé.');
        this.loadAbsenceTypes();
      },
      error: () => this.fail("Le statut du type n'a pas pu être modifié."),
    });
  }

  private reloadAll(): void {
    this.loadUsers();
    this.loadTeams();
    this.loadAbsenceTypes();
  }

  private loadUsers(): void {
    this.adminService.listUsers().subscribe({
      next: users => this.users.set(users),
      error: () => this.fail("Les utilisateurs n'ont pas pu être chargés."),
    });
  }

  private loadTeams(): void {
    this.adminService.listTeams().subscribe({
      next: teams => this.teams.set(teams),
      error: () => this.fail("Les équipes n'ont pas pu être chargées."),
    });
  }

  private loadAbsenceTypes(): void {
    this.adminService.listAbsenceTypes().subscribe({
      next: types => this.absenceTypes.set(types),
      error: () => this.fail("Les types d'absence n'ont pas pu être chargés."),
    });
  }

  private start(): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private done(message: string): void {
    this.busy.set(false);
    this.successMessage.set(message);
  }

  private fail(message: string): void {
    this.busy.set(false);
    this.errorMessage.set(message);
  }
}

function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
