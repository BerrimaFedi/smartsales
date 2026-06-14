import { Component, OnInit, inject, signal } from '@angular/core';
import { forkJoin, catchError, of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CommercialService } from '../../core/services/commercial.service';
import { ClientService } from '../../core/services/client.service';
import { VisiteService } from '../../core/services/visite.service';
import { Role } from '../../core/models/auth.model';
import { StatutVisite } from '../../core/models/visite.model';
import { DashboardCharts } from './dashboard-charts';

interface ManagerStats {
  commerciaux: number | null;
  clients: number | null;
  visitesAujourdhui: number | null;
  caMois: number | null;
}

interface CommercialStats {
  visitesAujourdhui: number;
  visitesTotales: number;
  visitesTerminees: number;
  caMois: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DashboardCharts],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly commercialSvc = inject(CommercialService);
  private readonly clientSvc = inject(ClientService);
  private readonly visiteSvc = inject(VisiteService);

  readonly isManager = this.auth.hasRole(Role.ADMIN, Role.MANAGER);
  readonly username = this.auth.currentUser()?.username ?? '';
  readonly roleLabel: string;
  readonly loading = signal(true);
  readonly managerStats = signal<ManagerStats>({ commerciaux: null, clients: null, visitesAujourdhui: null, caMois: null });
  readonly commercialStats = signal<CommercialStats | null>(null);

  constructor() {
    const map: Record<string, string> = { ADMIN: 'Administrateur', MANAGER: 'Manager', COMMERCIAL: 'Commercial' };
    this.roleLabel = map[this.auth.getRole() ?? ''] ?? '';
  }

  ngOnInit(): void {
    const today = new Date().toISOString().split('T')[0];
    const period = today.substring(0, 7);

    if (this.isManager) {
      forkJoin({
        commerciaux: this.commercialSvc.getAll().pipe(catchError(() => of(null))),
        clients:     this.clientSvc.getAll().pipe(catchError(() => of(null))),
        visites:     this.visiteSvc.getAll().pipe(catchError(() => of(null))),
      }).subscribe(({ commerciaux, clients, visites }) => {
        const visitesAujourdhui = visites
          ? visites.filter(v => v.dateVisite.startsWith(today)).length
          : null;
        const caMois = visites
          ? visites
              .filter(v => v.statut === StatutVisite.TERMINEE && v.dateVisite.startsWith(period))
              .reduce((s, v) => s + Number(v.montant || 0), 0)
          : null;
        this.managerStats.set({
          commerciaux: commerciaux ? commerciaux.length : null,
          clients:     clients ? clients.length : null,
          visitesAujourdhui,
          caMois,
        });
        this.loading.set(false);
      });
    } else {
      this.visiteSvc.getAll().pipe(catchError(() => of([]))).subscribe(visites => {
        const visitesAujourdhui = visites.filter(v => v.dateVisite.startsWith(today)).length;
        const visitesTotales    = visites.filter(v => v.dateVisite.startsWith(period)).length;
        const terminees         = visites.filter(v =>
          v.statut === StatutVisite.TERMINEE && v.dateVisite.startsWith(period));
        const visitesTerminees  = terminees.length;
        const caMois            = terminees.reduce((s, v) => s + Number(v.montant || 0), 0);
        this.commercialStats.set({ visitesAujourdhui, visitesTotales, visitesTerminees, caMois });
        this.loading.set(false);
      });
    }
  }

  formatCA(val: number | null): string {
    if (val === null) return '-';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(val) + ' DT';
  }
}
