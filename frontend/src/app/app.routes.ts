import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { roleGuard } from './core/auth/guards/role.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'callback',
    loadComponent: () =>
      import('./core/auth/callback.component').then(m => m.CallbackComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'my-absences' },
      {
        path: 'my-absences',
        title: 'Mes demandes d\'absence',
        loadComponent: () =>
          import('./features/my-absences/my-absences.component').then(m => m.MyAbsencesComponent),
      },
      {
        path: 'validation',
        title: 'Demandes à valider',
        canActivate: [roleGuard],
        data: { roles: ['MANAGER'] },
        loadComponent: () =>
          import('./features/validation/validation.component').then(m => m.ValidationComponent),
      },
      {
        path: 'planning',
        title: 'Planning mensuel',
        canActivate: [roleGuard],
        data: { roles: ['MANAGER', 'RH'] },
        loadComponent: () =>
          import('./features/planning/planning.component').then(m => m.PlanningComponent),
      },
      {
        path: 'payroll',
        title: 'RH et paie',
        canActivate: [roleGuard],
        data: { roles: ['RH'] },
        loadComponent: () =>
          import('./features/payroll/payroll.component').then(m => m.PayrollComponent),
      },
      {
        path: 'recurrences',
        title: 'Récurrences',
        loadComponent: () =>
          import('./features/recurrence/recurrence.component').then(m => m.RecurrenceComponent),
      },
      {
        path: 'admin',
        title: 'Administration',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/admin.component').then(m => m.AdminComponent),
      },
      {
        path: 'counters',
        title: 'Mes compteurs',
        loadComponent: () =>
          import('./features/counters/counters.component').then(m => m.CountersComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
