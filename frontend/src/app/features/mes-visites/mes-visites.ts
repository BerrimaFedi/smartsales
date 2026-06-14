import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LowerCasePipe } from '@angular/common';
import { VisiteService } from '../../core/services/visite.service';
import { Visite, StatutVisite } from '../../core/models/visite.model';

@Component({
  selector: 'app-mes-visites',
  standalone: true,
  imports: [ReactiveFormsModule, LowerCasePipe],
  templateUrl: './mes-visites.html',
  styleUrl: './mes-visites.scss',
})
export class MesVisites implements OnInit {
  private readonly svc = inject(VisiteService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly visites = signal<Visite[]>([]);
  readonly showForm = signal(false);
  readonly editingVisite = signal<Visite | null>(null);

  readonly statutOptions = Object.values(StatutVisite);
  readonly StatutVisite = StatutVisite;

  form!: FormGroup;

  // Signal mis à jour via valueChanges — seule façon de brancher un FormControl sur computed()
  readonly currentStatut = signal<StatutVisite | null>(null);
  readonly showMontant = computed(() => this.currentStatut() === StatutVisite.TERMINEE);

  ngOnInit(): void {
    this.form = this.fb.group({
      statut:      [null as StatutVisite | null, Validators.required],
      compteRendu: [''],
      montant:     [null as number | null, [Validators.min(0)]],
    });

    this.form.get('statut')!.valueChanges.subscribe(
      (v: StatutVisite | null) => this.currentStatut.set(v)
    );

    this.svc.getAll().subscribe({
      next: visites => {
        this.visites.set(visites.sort((a, b) => b.dateVisite.localeCompare(a.dateVisite)));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les visites.');
        this.loading.set(false);
      },
    });
  }

  openEdit(v: Visite): void {
    this.editingVisite.set(v);
    this.error.set(null);
    this.form.reset({
      statut:      v.statut,
      compteRendu: v.compteRendu ?? '',
      montant:     v.montant > 0 ? v.montant : null,
    });
    // form.reset() émet valueChanges mais on force ici pour garantir la cohérence initiale
    this.currentStatut.set(v.statut);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.error.set(null);
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const visite = this.editingVisite();
    if (!visite) return;

    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();

    const montantVal = raw['statut'] === StatutVisite.TERMINEE && raw['montant'] != null
      ? Number(raw['montant'])
      : null;

    this.svc.patch(visite.id, {
      statut:      raw['statut'],
      compteRendu: raw['compteRendu'] || null,
      montant:     montantVal,
    }).subscribe({
      next: updated => {
        this.visites.update(list => list.map(v => (v.id === updated.id ? updated : v)));
        this.saving.set(false);
        this.showForm.set(false);
      },
      error: () => {
        this.error.set('Erreur lors de la sauvegarde.');
        this.saving.set(false);
      },
    });
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
}
