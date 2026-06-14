import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { CompetenceService } from '../../core/services/competence.service';
import { Competence } from '../../core/models/competence.model';
import { Role } from '../../core/models/auth.model';

@Component({
  selector: 'app-competences',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './competences.html',
  styleUrl: './competences.scss',
})
export class Competences implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly svc = inject(CompetenceService);
  private readonly fb = inject(FormBuilder);

  readonly canWrite = this.auth.hasRole(Role.ADMIN, Role.MANAGER);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly deleteConfirmId = signal<number | null>(null);

  readonly competences = signal<Competence[]>([]);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      nom: ['', Validators.required],
    });

    this.svc.getAll().subscribe({
      next: competences => {
        this.competences.set(competences);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les compétences.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.error.set(null);
    this.form.reset({ nom: '' });
    this.showForm.set(true);
  }

  openEdit(c: Competence): void {
    this.editingId.set(c.id);
    this.error.set(null);
    this.form.reset({ nom: c.nom });
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

    const req = { nom: this.form.getRawValue()['nom'] as string };
    const id = this.editingId();
    const op$ = id ? this.svc.update(id, req) : this.svc.create(req);

    op$.subscribe({
      next: saved => {
        this.competences.update(list =>
          id ? list.map(c => (c.id === id ? saved : c)) : [...list, saved]
        );
        this.saving.set(false);
        this.showForm.set(false);
      },
      error: () => {
        this.error.set('Erreur lors de la sauvegarde. Le nom doit être unique.');
        this.saving.set(false);
      },
    });
  }

  askDelete(id: number): void { this.deleteConfirmId.set(id); }
  cancelDelete(): void { this.deleteConfirmId.set(null); }

  confirmDelete(id: number): void {
    this.svc.delete(id).subscribe({
      next: () => {
        this.competences.update(list => list.filter(c => c.id !== id));
        this.deleteConfirmId.set(null);
      },
      error: () => this.error.set('Erreur lors de la suppression.'),
    });
  }
}
