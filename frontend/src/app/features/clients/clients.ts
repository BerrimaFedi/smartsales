import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ClientService } from '../../core/services/client.service';
import { ZoneService } from '../../core/services/zone.service';
import { Client } from '../../core/models/client.model';
import { Zone } from '../../core/models/zone.model';
import { Role } from '../../core/models/auth.model';

@Component({
  selector: 'app-clients',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './clients.html',
  styleUrl: './clients.scss',
})
export class Clients implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly svc = inject(ClientService);
  private readonly zoneSvc = inject(ZoneService);
  private readonly fb = inject(FormBuilder);

  readonly canWrite = this.auth.hasRole(Role.ADMIN, Role.MANAGER);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly deleteConfirmId = signal<number | null>(null);

  readonly clients = signal<Client[]>([]);
  readonly zones = signal<Zone[]>([]);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      nom:       ['', Validators.required],
      adresse:   [''],
      telephone: [''],
      latitude:  [null as number | null],
      longitude: [null as number | null],
      zoneId:    [null as number | null],
    });

    forkJoin({
      clients: this.svc.getAll(),
      zones:   this.zoneSvc.getAll(),
    }).subscribe({
      next: ({ clients, zones }) => {
        this.clients.set(clients);
        this.zones.set(zones);
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
    this.form.reset({ nom: '', adresse: '', telephone: '', latitude: null, longitude: null, zoneId: null });
    this.showForm.set(true);
  }

  openEdit(c: Client): void {
    this.editingId.set(c.id);
    this.form.reset({
      nom:       c.nom,
      adresse:   c.adresse ?? '',
      telephone: c.telephone ?? '',
      latitude:  c.latitude ?? null,
      longitude: c.longitude ?? null,
      zoneId:    c.zone?.id ?? null,
    });
    this.showForm.set(true);
  }

  cancelForm(): void { this.showForm.set(false); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.error.set(null);

    const raw = this.form.getRawValue();
    const req = {
      nom:       raw['nom'],
      adresse:   raw['adresse'] || undefined,
      telephone: raw['telephone'] || undefined,
      latitude:  raw['latitude'] !== null && raw['latitude'] !== '' ? Number(raw['latitude']) : undefined,
      longitude: raw['longitude'] !== null && raw['longitude'] !== '' ? Number(raw['longitude']) : undefined,
      zoneId:    raw['zoneId'] ? Number(raw['zoneId']) : undefined,
    };

    const id = this.editingId();
    const op$ = id ? this.svc.update(id, req) : this.svc.create(req);

    op$.subscribe({
      next: saved => {
        this.clients.update(list =>
          id ? list.map(c => (c.id === id ? saved : c)) : [...list, saved]
        );
        this.saving.set(false);
        this.showForm.set(false);
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
        this.clients.update(list => list.filter(c => c.id !== id));
        this.deleteConfirmId.set(null);
      },
      error: () => this.error.set('Erreur lors de la suppression.'),
    });
  }

  formatCoord(val: number | null): string {
    return val !== null ? val.toFixed(4) : '—';
  }
}
