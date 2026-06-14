import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Role } from './core/models/auth.model';
import { Login } from './features/auth/login/login';
import { Shell } from './layout/shell/shell';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard').then(m => m.Dashboard),
      },
      {
        path: 'commerciaux',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER])],
        loadComponent: () =>
          import('./features/commerciaux/commerciaux').then(m => m.Commerciaux),
      },
      {
        path: 'clients',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER])],
        loadComponent: () =>
          import('./features/clients/clients').then(m => m.Clients),
      },
      {
        path: 'zones',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER])],
        loadComponent: () =>
          import('./features/zones/zones').then(m => m.Zones),
      },
      {
        path: 'competences',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER])],
        loadComponent: () =>
          import('./features/competences/competences').then(m => m.Competences),
      },
      {
        path: 'calendrier',
        loadComponent: () =>
          import('./features/calendrier/calendrier').then(m => m.Calendrier),
      },
      {
        path: 'mes-visites',
        canActivate: [roleGuard([Role.COMMERCIAL])],
        loadComponent: () =>
          import('./features/mes-visites/mes-visites').then(m => m.MesVisites),
      },
      {
        path: 'planning',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER, Role.COMMERCIAL])],
        loadComponent: () =>
          import('./features/planning/planning').then(m => m.Planning),
      },
      {
        path: 'performances',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER, Role.COMMERCIAL])],
        loadComponent: () =>
          import('./features/performances/performances').then(m => m.Performances),
      },
      {
        path: 'toutes-visites',
        canActivate: [roleGuard([Role.ADMIN, Role.MANAGER])],
        loadComponent: () =>
          import('./features/toutes-visites/toutes-visites').then(m => m.ToutesVisites),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
