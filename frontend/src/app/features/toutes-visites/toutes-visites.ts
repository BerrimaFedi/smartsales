import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LowerCasePipe } from '@angular/common';
import { VisiteService } from '../../core/services/visite.service';
import { CommercialService } from '../../core/services/commercial.service';
import { Visite, StatutVisite, TypeVisite } from '../../core/models/visite.model';
import { Commercial } from '../../core/models/commercial.model';
import { forkJoin, catchError, of } from 'rxjs';

@Component({
  selector: 'app-toutes-visites',
  standalone: true,
  imports: [FormsModule, LowerCasePipe],
  templateUrl: './toutes-visites.html',
  styleUrl: './toutes-visites.scss',
})
export class ToutesVisites implements OnInit {
  private readonly visiteSvc = inject(VisiteService);
  private readonly commercialSvc = inject(CommercialService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly visites = signal<Visite[]>([]);
  readonly commerciaux = signal<Commercial[]>([]);

  readonly filterCommercialId = signal<number | null>(null);
  readonly filterStatut = signal<string>('');
  readonly filterDateMin = signal<string>('');
  readonly filterDateMax = signal<string>('');

  readonly statutOptions = Object.values(StatutVisite);

  readonly filtered = computed(() => {
    let list = this.visites();
    const comId = this.filterCommercialId();
    if (comId !== null) list = list.filter(v => v.commercialId === comId);
    const statut = this.filterStatut();
    if (statut) list = list.filter(v => v.statut === statut);
    const dMin = this.filterDateMin();
    if (dMin) list = list.filter(v => v.dateVisite >= dMin);
    const dMax = this.filterDateMax();
    if (dMax) list = list.filter(v => v.dateVisite <= dMax + 'T23:59:59');
    return list;
  });

  ngOnInit(): void {
    forkJoin({
      visites:     this.visiteSvc.getAll().pipe(catchError(() => of([]))),
      commerciaux: this.commercialSvc.getAll().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ visites, commerciaux }) => {
        this.visites.set(visites.sort((a, b) => b.dateVisite.localeCompare(a.dateVisite)));
        this.commerciaux.set(commerciaux);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les visites.');
        this.loading.set(false);
      },
    });
  }

  onCommercialFilter(event: Event): void {
    const v = (event.target as HTMLSelectElement).value;
    this.filterCommercialId.set(v ? Number(v) : null);
  }

  onStatutFilter(event: Event): void {
    this.filterStatut.set((event.target as HTMLSelectElement).value);
  }

  onDateMinFilter(event: Event): void {
    this.filterDateMin.set((event.target as HTMLInputElement).value);
  }

  onDateMaxFilter(event: Event): void {
    this.filterDateMax.set((event.target as HTMLInputElement).value);
  }

  resetFiltres(): void {
    this.filterCommercialId.set(null);
    this.filterStatut.set('');
    this.filterDateMin.set('');
    this.filterDateMax.set('');
  }

  formatMontant(m: number): string {
    if (!m || m === 0) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(m) + ' DT';
  }

  statutLabel(s: string): string {
    const map: Record<string, string> = {
      PLANIFIEE: 'Planifiée', EN_COURS: 'En cours', TERMINEE: 'Terminée', ANNULEE: 'Annulée',
    };
    return map[s] ?? s;
  }

  typeLabel(t: string): string {
    const map: Record<string, string> = {
      PROSPECTION: 'Prospection', RELANCE: 'Relance', NEGOCIATION: 'Négociation',
    };
    return map[t] ?? t;
  }

  formatDateHeure(dt: string): string {
    try {
      const d = new Date(dt);
      return d.toLocaleDateString('fr-FR') + ' ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    } catch { return dt; }
  }

  caTotal(): number {
    return this.filtered()
      .filter(v => v.statut === StatutVisite.TERMINEE)
      .reduce((s, v) => s + Number(v.montant || 0), 0);
  }

  formatCA(n: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' DT';
  }
}
