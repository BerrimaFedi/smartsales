import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CommercialService } from '../../core/services/commercial.service';
import { ZoneService } from '../../core/services/zone.service';
import { CompetenceService } from '../../core/services/competence.service';
import { Commercial, CommercialRequest } from '../../core/models/commercial.model';
import { Zone } from '../../core/models/zone.model';
import { Competence } from '../../core/models/competence.model';
import { Role } from '../../core/models/auth.model';

@Component({
  selector: 'app-commerciaux',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './commerciaux.html',
  styleUrl: './commerciaux.scss',
})
export class Commerciaux implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly svc = inject(CommercialService);
  private readonly zoneSvc = inject(ZoneService);
  private readonly compSvc = inject(CompetenceService);
  private readonly fb = inject(FormBuilder);

  readonly canWrite = this.auth.hasRole(Role.ADMIN, Role.MANAGER);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly deleteConfirmId = signal<number | null>(null);

  readonly commerciaux = signal<Commercial[]>([]);
  readonly zones = signal<Zone[]>([]);
  readonly competences = signal<Competence[]>([]);
  readonly selectedCompIds = signal<number[]>([]);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      nom:       ['', Validators.required],
      prenom:    ['', Validators.required],
      telephone: [''],
      zoneId:    [null as number | null],
      username:  ['', Validators.required],
      email:     ['', [Validators.required, Validators.email]],
      password:  ['', Validators.required],   // requis à la création ; retiré à l'édition
    });

    forkJoin({
      commerciaux:  this.svc.getAll(),
      zones:        this.zoneSvc.getAll(),
      competences:  this.compSvc.getAll(),
    }).subscribe({
      next: ({ commerciaux, zones, competences }) => {
        this.commerciaux.set(commerciaux);
        this.zones.set(zones);
        this.competences.set(competences);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les données.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.selectedCompIds.set([]);
    this.error.set(null);
    // Password obligatoire à la création
    this.form.get('password')!.setValidators(Validators.required);
    this.form.get('password')!.updateValueAndValidity();
    this.form.reset({ nom: '', prenom: '', telephone: '', zoneId: null, username: '', email: '', password: '' });
    this.showForm.set(true);
  }

  openEdit(c: Commercial): void {
    this.editingId.set(c.id);
    this.selectedCompIds.set(c.competences.map(cp => cp.id));
    this.error.set(null);
    // Password optionnel à l'édition
    this.form.get('password')!.clearValidators();
    this.form.get('password')!.updateValueAndValidity();
    this.form.reset({
      nom:       c.nom,
      prenom:    c.prenom,
      telephone: c.telephone ?? '',
      zoneId:    c.zone?.id ?? null,
      username:  c.username ?? '',
      email:     c.email ?? '',
      password:  '',
    });
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.error.set(null);
  }

  toggleComp(id: number): void {
    this.selectedCompIds.update(ids =>
      ids.includes(id) ? ids.filter(i => i !== id) : [...ids, id]
    );
  }

  isCompSelected(id: number): boolean { return this.selectedCompIds().includes(id); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.error.set(null);

    const raw = this.form.getRawValue();
    const req: CommercialRequest = {
      nom:           raw['nom'],
      prenom:        raw['prenom'],
      telephone:     raw['telephone'] || undefined,
      zoneId:        raw['zoneId'] ? Number(raw['zoneId']) : undefined,
      competenceIds: this.selectedCompIds(),
      username:      raw['username'],
      email:         raw['email'],
      password:      raw['password'] || undefined,  // undefined → clé absente → inchangé côté Java
    };

    const id = this.editingId();
    const op$ = id ? this.svc.update(id, req) : this.svc.create(req);

    op$.subscribe({
      next: saved => {
        this.commerciaux.update(list =>
          id ? list.map(c => (c.id === id ? saved : c)) : [...list, saved]
        );
        this.saving.set(false);
        this.showForm.set(false);
      },
      error: (err: any) => {
        const body = err?.error;
        let msg = 'Erreur lors de la sauvegarde.';
        if (body?.message && typeof body.message === 'string') {
          msg = body.message;
        } else if (body?.message && typeof body.message === 'object') {
          // Erreurs de validation Bean (map champ → message)
          msg = Object.values(body.message as Record<string, string>).join(' • ');
        }
        this.error.set(msg);
        this.saving.set(false);
      },
    });
  }

  askDelete(id: number): void { this.deleteConfirmId.set(id); }
  cancelDelete(): void { this.deleteConfirmId.set(null); }

  confirmDelete(id: number): void {
    this.svc.delete(id).subscribe({
      next: () => {
        this.commerciaux.update(list => list.filter(c => c.id !== id));
        this.deleteConfirmId.set(null);
      },
      error: () => this.error.set('Erreur lors de la suppression.'),
    });
  }

  competenceLabels(c: Commercial): string {
    return c.competences.map(cp => cp.nom).join(', ') || '—';
  }
}
