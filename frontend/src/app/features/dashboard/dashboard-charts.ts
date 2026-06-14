import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardStats } from '../../core/models/dashboard-stats.model';

// Enregistre tous les composants Chart.js une seule fois au chargement du module.
Chart.register(...registerables);

/** Formate un nombre en "13 150 DT" selon la locale française. */
function formatCA(val: number): string {
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(val) + ' DT';
}

/** Convertit "AAAA-MM" en étiquette courte, ex. "Jan 2025". */
function labelMois(periode: string): string {
  const [year, month] = periode.split('-');
  const noms = ['', 'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun',
                 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];
  return `${noms[parseInt(month, 10)]} ${year}`;
}

@Component({
  selector: 'app-dashboard-charts',
  standalone: true,
  imports: [],
  templateUrl: './dashboard-charts.html',
  styleUrl:    './dashboard-charts.scss',
})
export class DashboardCharts implements OnInit, OnDestroy {
  private readonly svc = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  // ── Canvas refs (static: false — mis à jour après chaque CD) ────────────────
  @ViewChild('canvasBarCA')          private canvasBarCA!:          ElementRef<HTMLCanvasElement>;
  @ViewChild('canvasDoughnutStatut') private canvasDoughnutStatut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('canvasDoughnutType')   private canvasDoughnutType!:   ElementRef<HTMLCanvasElement>;
  @ViewChild('canvasLineCA')         private canvasLineCA!:         ElementRef<HTMLCanvasElement>;

  // ── Signaux exposés au template ──────────────────────────────────────────────
  readonly loading = signal(true);
  readonly erreur  = signal<string | null>(null);
  readonly stats   = signal<DashboardStats | null>(null);

  // ── Instances Chart.js (à détruire dans ngOnDestroy) ─────────────────────────
  private charts: (Chart | null)[] = [null, null, null, null];

  ngOnInit(): void {
    this.svc.getStats().subscribe({
      next: data => {
        this.stats.set(data);
        this.loading.set(false);
        // detectChanges() force Angular à re-rendre le @else immédiatement (canvas insérés + @ViewChild mis à jour)
        // avant d'appeler dessinerTout(), évitant le race condition setTimeout/signal-CD.
        this.cdr.detectChanges();
        this.dessinerTout();
      },
      error: () => {
        this.erreur.set('Impossible de charger les statistiques graphiques.');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    // Détruit proprement chaque instance pour éviter les duplications au re-render.
    this.charts.forEach(c => c?.destroy());
  }

  // ── Getters d'aide pour les états vides (utilisés dans le template) ──────────

  get barVide(): boolean {
    return (this.stats()?.caParCommercial?.length ?? 0) === 0;
  }

  get statutVide(): boolean {
    const s = this.stats()?.visitesParStatut;
    return !s || (s.planifiee + s.enCours + s.terminee + s.annulee) === 0;
  }

  get typeVide(): boolean {
    const t = this.stats()?.visitesParType;
    return !t || (t.prospection + t.relance + t.negociation) === 0;
  }

  // ── Dessin des 4 graphiques ───────────────────────────────────────────────────

  private dessinerTout(): void {
    const s = this.stats();
    if (!s) return;
    this.charts.forEach(c => c?.destroy());
    this.charts = [
      this.dessinerBarCA(s),
      this.dessinerDoughnutStatut(s),
      this.dessinerDoughnutType(s),
      this.dessinerLineCA(s),
    ];
  }

  /** Graphique 1 — Barres : CA par commercial du mois courant. */
  private dessinerBarCA(s: DashboardStats): Chart | null {
    if (!this.canvasBarCA || this.barVide) return null;
    return new Chart(this.canvasBarCA.nativeElement, {
      type: 'bar',
      data: {
        labels: s.caParCommercial.map(d => d.commercial),
        datasets: [{
          label: 'CA (DT)',
          data:  s.caParCommercial.map(d => d.ca),
          backgroundColor: 'rgba(37,99,235,0.85)',
          borderRadius: 6,
          borderSkipped: false,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: ctx => ' ' + formatCA(ctx.parsed.y ?? 0) } },
        },
        scales: {
          y: {
            beginAtZero: true,
            grid:  { color: '#f3f4f6' },
            ticks: { callback: val => formatCA(val as number) },
          },
          x: { grid: { display: false } },
        },
      },
    });
  }

  /** Graphique 2 — Doughnut : répartition des visites par statut. */
  private dessinerDoughnutStatut(s: DashboardStats): Chart | null {
    if (!this.canvasDoughnutStatut || this.statutVide) return null;
    const st = s.visitesParStatut;
    return new Chart(this.canvasDoughnutStatut.nativeElement, {
      type: 'doughnut',
      data: {
        labels:   ['Planifiée', 'En cours', 'Terminée', 'Annulée'],
        datasets: [{
          data:            [st.planifiee, st.enCours, st.terminee, st.annulee],
          backgroundColor: ['#3b82f6', '#f97316', '#22c55e', '#94a3b8'],
          borderWidth: 2,
          borderColor: '#ffffff',
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: { position: 'bottom', labels: { padding: 14, font: { size: 12 } } },
        },
      },
    });
  }

  /** Graphique 3 — Doughnut : répartition des visites par type. */
  private dessinerDoughnutType(s: DashboardStats): Chart | null {
    if (!this.canvasDoughnutType || this.typeVide) return null;
    const t = s.visitesParType;
    return new Chart(this.canvasDoughnutType.nativeElement, {
      type: 'doughnut',
      data: {
        labels:   ['Prospection', 'Relance', 'Négociation'],
        datasets: [{
          data:            [t.prospection, t.relance, t.negociation],
          backgroundColor: ['#2563eb', '#8b5cf6', '#06b6d4'],
          borderWidth: 2,
          borderColor: '#ffffff',
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: { position: 'bottom', labels: { padding: 14, font: { size: 12 } } },
        },
      },
    });
  }

  /** Graphique 4 — Courbe : évolution du CA de l'équipe sur 6 mois. */
  private dessinerLineCA(s: DashboardStats): Chart | null {
    if (!this.canvasLineCA) return null;
    return new Chart(this.canvasLineCA.nativeElement, {
      type: 'line',
      data: {
        labels:   s.caParMois.map(d => labelMois(d.periode)),
        datasets: [{
          label:           'CA équipe',
          data:            s.caParMois.map(d => d.ca),
          borderColor:     '#2563eb',
          backgroundColor: 'rgba(37,99,235,0.08)',
          tension:         0.4,
          fill:            true,
          pointBackgroundColor: '#2563eb',
          pointRadius:     4,
          pointHoverRadius: 6,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: ctx => ' ' + formatCA(ctx.parsed.y ?? 0) } },
        },
        scales: {
          y: {
            beginAtZero: true,
            grid:  { color: '#f3f4f6' },
            ticks: { callback: val => formatCA(val as number) },
          },
          x: { grid: { display: false } },
        },
      },
    });
  }
}
