import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ZoneService } from '../../core/services/zone.service';
import { Zone } from '../../core/models/zone.model';
import { Role } from '../../core/models/auth.model';

@Component({
  selector: 'app-zones',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './zones.html',
  styleUrl: './zones.scss',
})
export class Zones implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly svc = inject(ZoneService);
  private readonly fb = inject(FormBuilder);

  readonly canWrite = this.auth.hasRole(Role.ADMIN, Role.MANAGER);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly deleteConfirmId = signal<number | null>(null);

  readonly zones = signal<Zone[]>([]);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      nom:         ['', Validators.required],
      description: [''],
    });

    this.svc.getAll().subscribe({
      next: zones => {
        this.zones.set(zones);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les zones.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.error.set(null);
    this.form.reset({ nom: '', description: '' });
    this.showForm.set(true);
  }

  openEdit(z: Zone): void {
    this.editingId.set(z.id);
    this.error.set(null);
    this.form.reset({ nom: z.nom, description: z.description ?? '' });
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
    const req = {
      nom:         raw['nom'],
      description: raw['description'] || undefined,
    };

    const id = this.editingId();
    const op$ = id ? this.svc.update(id, req) : this.svc.create(req);

    op$.subscribe({
      next: saved => {
        this.zones.update(list =>
          id ? list.map(z => (z.id === id ? saved : z)) : [...list, saved]
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
        this.zones.update(list => list.filter(z => z.id !== id));
        this.deleteConfirmId.set(null);
      },
      error: () => this.error.set('Erreur lors de la suppression.'),
    });
  }
}
