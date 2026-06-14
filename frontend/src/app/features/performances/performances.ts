import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { PerformanceService } from '../../core/services/performance.service';
import { CommercialService } from '../../core/services/commercial.service';
import { AnalysePerformance, PerformanceCalculee, PerformanceRequest } from '../../core/models/performance.model';
import { Commercial } from '../../core/models/commercial.model';
import { Role } from '../../core/models/auth.model';
import { forkJoin, catchError, of } from 'rxjs';

interface BarItem {
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  value: number;
}

@Component({
  selector: 'app-performances',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './performances.html',
  styleUrl: './performances.scss',
})
export class Performances implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly svc = inject(PerformanceService);
  private readonly commercialSvc = inject(CommercialService);
  private readonly fb = inject(FormBuilder);

  readonly isManager = this.auth.hasRole(Role.ADMIN, Role.MANAGER);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly deleteConfirmId = signal<number | null>(null);

  readonly analyse = signal<AnalysePerformance | null>(null);
  readonly analyseLoading = signal(false);
  readonly analyseError = signal<string | null>(null);
  readonly analyseUnavailable = signal(false);

  readonly calculeesData = signal<PerformanceCalculee[]>([]);
  readonly commerciaux = signal<Commercial[]>([]);

  readonly filterCommercialId = signal<number | null>(null);
  readonly filterPeriode = signal(this.moisCourant());

  form!: FormGroup;

  readonly filtered = computed(() => {
    let list = this.calculeesData();
    const selId = this.filterCommercialId();
    if (selId !== null) list = list.filter(p => p.commercialId === selId);
    return list;
  });

  readonly chartBars = computed<BarItem[] | null>(() => {
    const data = this.filtered();
    if (!data.length) return null;

    const maxCA = Math.max(...data.map(p => Number(p.chiffreAffaires)));
    if (maxCA === 0) return null;

    const CHART_W = 560;
    const CHART_H = 160;
    const n = data.length;
    const barW = Math.max(20, Math.min(60, CHART_W / (n * 1.6)));
    const gap = (CHART_W - n * barW) / (n + 1);

    return data.map((p, i) => {
      const h = (Number(p.chiffreAffaires) / maxCA) * CHART_H;
      return {
        x: gap + i * (barW + gap),
        y: CHART_H - h,
        width: barW,
        height: h,
        label: p.commercialNom.split(' ')[0].substring(0, 9),
        value: Number(p.chiffreAffaires),
      };
    });
  });

  private moisCourant(): string {
    return new Date().toISOString().substring(0, 7);
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      commercialId:    [null as number | null, Validators.required],
      periode:         ['', Validators.required],
      chiffreAffaires: [null as number | null, [Validators.required, Validators.min(0)]],
      nombreVisites:   [null as number | null, [Validators.required, Validators.min(0)]],
      tauxConversion:  [null as number | null, [Validators.required, Validators.min(0), Validators.max(100)]],
    });

    this.loadData();

    if (!this.isManager) {
      this.chargerAnalyseMe();
    }
  }

  private loadData(): void {
    this.loading.set(true);
    const periode = this.filterPeriode();

    if (this.isManager) {
      forkJoin({
        calculees:   this.svc.getCalculees(periode).pipe(catchError(() => of([]))),
        commerciaux: this.commercialSvc.getAll().pipe(catchError(() => of([]))),
      }).subscribe({
        next: ({ calculees, commerciaux }) => {
          this.calculeesData.set(calculees);
          this.commerciaux.set(commerciaux);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Erreur lors du chargement.');
          this.loading.set(false);
        },
      });
    } else {
      this.svc.getCalculeeMe(periode).pipe(catchError(() => of(null))).subscribe({
        next: calcuee => {
          this.calculeesData.set(calcuee ? [calcuee] : []);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Erreur lors du chargement.');
          this.loading.set(false);
        },
      });
    }
  }

  onCommercialFilter(event: Event): void {
    const v = (event.target as HTMLSelectElement).value;
    const id = v ? Number(v) : null;
    this.filterCommercialId.set(id);
    if (id) {
      this.chargerAnalyse(id);
    } else {
      this.analyse.set(null);
      this.analyseError.set(null);
      this.analyseUnavailable.set(false);
    }
  }

  onPeriodeFilter(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.filterPeriode.set(val || this.moisCourant());
    this.loadData();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.error.set(null);
    this.form.reset({
      commercialId: null, periode: this.filterPeriode(), chiffreAffaires: null,
      nombreVisites: null, tauxConversion: null,
    });
    this.showForm.set(true);
  }

  openEdit(p: PerformanceCalculee): void {
    if (!p.performanceManuelleId) return;
    this.editingId.set(p.performanceManuelleId);
    this.error.set(null);
    this.form.reset({
      commercialId:    p.commercialId,
      periode:         p.periode,
      chiffreAffaires: Number(p.chiffreAffaires),
      nombreVisites:   p.nombreVisites,
      tauxConversion:  Number((p.tauxConversion * 100).toFixed(2)),
    });
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.error.set(null);
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.error.set(null);

    const raw = this.form.getRawValue();
    const req: PerformanceRequest = {
      commercialId:    Number(raw['commercialId']),
      periode:         raw['periode'],
      chiffreAffaires: Number(raw['chiffreAffaires']),
      nombreVisites:   Number(raw['nombreVisites']),
      tauxConversion:  Number(raw['tauxConversion']) / 100,
    };

    const id = this.editingId();
    const op$ = id ? this.svc.update(id, req) : this.svc.create(req);

    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.loadData();
      },
      error: () => {
        this.error.set('Erreur lors de la sauvegarde.');
        this.saving.set(false);
      },
    });
  }

  askDelete(id: number): void { this.deleteConfirmId.set(id); }
  cancelDelete(): void { this.deleteConfirmId.set(null); }

  confirmDelete(id: number): void {
    this.svc.delete(id).subscribe({
      next: () => {
        this.deleteConfirmId.set(null);
        this.loadData();
      },
      error: () => this.error.set('Erreur lors de la suppression.'),
    });
  }

  exportCsv(): void {
    const periode = this.filterPeriode();
    this.svc.getRapportCsv(periode).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `performances-${periode}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set('Erreur lors de l\'export CSV.'),
    });
  }

  chargerAnalyse(commercialId: number): void {
    this.analyse.set(null);
    this.analyseError.set(null);
    this.analyseUnavailable.set(false);
    this.analyseLoading.set(true);
    this.svc.getAnalyse(commercialId).pipe(
      catchError((err: any) => {
        if (err?.status === 503) this.analyseUnavailable.set(true);
        else this.analyseError.set('Erreur lors du chargement de l\'analyse IA.');
        this.analyseLoading.set(false);
        return of(null);
      })
    ).subscribe({
      next: (a) => {
        this.analyse.set(a);
        this.analyseLoading.set(false);
      },
    });
  }

  chargerAnalyseMe(): void {
    this.analyseLoading.set(true);
    this.analyseError.set(null);
    this.analyseUnavailable.set(false);
    this.svc.getAnalyseMe().pipe(
      catchError((err: any) => {
        if (err?.status === 503) this.analyseUnavailable.set(true);
        else this.analyseError.set('Erreur lors du chargement de l\'analyse IA.');
        this.analyseLoading.set(false);
        return of(null);
      })
    ).subscribe({
      next: (a) => {
        this.analyse.set(a);
        this.analyseLoading.set(false);
      },
    });
  }

  tendanceBadgeClass(tendance: string): string {
    if (tendance === 'en hausse') return 'badge badge-hausse';
    if (tendance === 'en baisse') return 'badge badge-baisse';
    return 'badge badge-stable';
  }

  formatCA(n: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' DT';
  }

  formatRate(n: number): string {
    return (n * 100).toFixed(1) + ' %';
  }

  totalCA(): number {
    return this.filtered().reduce((s, p) => s + Number(p.chiffreAffaires), 0);
  }

  totalVisites(): number {
    return this.filtered().reduce((s, p) => s + p.nombreVisites, 0);
  }
}
