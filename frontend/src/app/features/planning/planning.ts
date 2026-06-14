import {
  AfterViewInit, Component, ElementRef, OnDestroy, OnInit,
  ViewChild, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LowerCasePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import * as L from 'leaflet';
import { AuthService } from '../../core/services/auth.service';
import { CommercialService } from '../../core/services/commercial.service';
import { ClientService } from '../../core/services/client.service';
import { VisiteService } from '../../core/services/visite.service';
import { PlanningService } from '../../core/services/planning.service';
import { Commercial } from '../../core/models/commercial.model';
import { Client } from '../../core/models/client.model';
import { Visite, VisiteRequest, TypeVisite, StatutVisite } from '../../core/models/visite.model';
import { AffectationRapport } from '../../core/models/planning.model';
import { Role } from '../../core/models/auth.model';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [ReactiveFormsModule, LowerCasePipe],
  templateUrl: './planning.html',
  styleUrl: './planning.scss',
})
export class Planning implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapEl') mapEl!: ElementRef<HTMLDivElement>;

  private readonly auth = inject(AuthService);
  private readonly commercialSvc = inject(CommercialService);
  private readonly clientSvc = inject(ClientService);
  private readonly visiteSvc = inject(VisiteService);
  private readonly planningSvc = inject(PlanningService);
  private readonly fb = inject(FormBuilder);

  readonly isManager = this.auth.hasRole(Role.ADMIN, Role.MANAGER);

  readonly commerciaux = signal<Commercial[]>([]);
  readonly clients = signal<Client[]>([]);
  readonly visites = signal<Visite[]>([]);
  readonly selectedCommercialId = signal<number | null>(null);
  readonly selectedDate = signal(new Date().toISOString().split('T')[0]);
  readonly optimizedVisites = signal<Visite[] | null>(null);
  readonly distanceTotale = signal<number | null>(null);

  readonly loading = signal(false);
  readonly optimizing = signal(false);
  readonly aiError = signal(false);
  readonly error = signal<string | null>(null);
  readonly showVisiteForm = signal(false);
  readonly savingVisite = signal(false);

  // — Affectation automatique —
  readonly showAffectationModal = signal(false);
  readonly affectationDate = signal(new Date().toISOString().split('T')[0]);
  readonly affectationLoading = signal(false);
  readonly affectationRapport = signal<AffectationRapport | null>(null);

  // — Réaffectation dynamique —
  readonly showReaffectationModal = signal(false);
  readonly reaffectationCommercialId = signal<number | null>(null);
  readonly reaffectationDate = signal(new Date().toISOString().split('T')[0]);
  readonly reaffectationLoading = signal(false);
  readonly reaffectationRapport = signal<AffectationRapport | null>(null);

  visiteForm!: FormGroup;

  readonly typeOptions = Object.values(TypeVisite);
  readonly statutOptions = Object.values(StatutVisite);

  private map!: L.Map;
  private markers: L.Marker[] = [];
  private polyline: L.Polyline | null = null;
  private mapReady = false;

  ngOnInit(): void {
    this.visiteForm = this.fb.group({
      commercialId: [null],  // optionnel : null = visite non assignée
      clientId:   [null, Validators.required],
      type:       [TypeVisite.PROSPECTION, Validators.required],
      statut:     [StatutVisite.PLANIFIEE],
      dateVisite: [this.selectedDate(), Validators.required],
      heure:      ['09:00', Validators.required],
    });

    if (this.isManager) {
      forkJoin({
        commerciaux: this.commercialSvc.getAll(),
        clients:     this.clientSvc.getAll(),
      }).subscribe({
        next: ({ commerciaux, clients }) => {
          this.commerciaux.set(commerciaux);
          this.clients.set(clients);
        },
        error: () => this.error.set('Erreur lors du chargement.'),
      });
    } else {
      forkJoin({
        me:      this.commercialSvc.getMe(),
        clients: this.clientSvc.getAll(),
      }).subscribe({
        next: ({ me, clients }) => {
          this.commerciaux.set([me]);
          this.clients.set(clients);
          this.selectedCommercialId.set(me.id);
          this.loadVisites();
        },
        error: () => this.error.set('Erreur lors du chargement.'),
      });
    }
  }

  ngAfterViewInit(): void {
    this.map = L.map(this.mapEl.nativeElement).setView([36.8065, 10.1815], 11);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(this.map);
    this.mapReady = true;
    if (this.visites().length > 0) this.refreshMap();
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }

  onCommercialChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    this.selectedCommercialId.set(id || null);
    this.resetOptimization();
    if (id) this.loadVisites();
  }

  onDateChange(event: Event): void {
    const date = (event.target as HTMLInputElement).value;
    this.selectedDate.set(date);
    this.visiteForm.patchValue({ dateVisite: date });
    this.resetOptimization();
    if (this.selectedCommercialId()) this.loadVisites();
  }

  loadVisites(): void {
    const id = this.selectedCommercialId();
    const date = this.selectedDate();
    if (!id || !date) return;

    this.loading.set(true);
    this.error.set(null);

    this.visiteSvc.getAll().subscribe({
      next: all => {
        const filtered = all
          .filter(v => v.commercialId === id && v.dateVisite.startsWith(date))
          .sort((a, b) => {
            if (a.ordreTournee !== null && b.ordreTournee !== null)
              return a.ordreTournee - b.ordreTournee;
            return a.dateVisite.localeCompare(b.dateVisite);
          });
        this.visites.set(filtered);
        this.loading.set(false);
        if (this.mapReady) this.refreshMap();
      },
      error: () => {
        this.error.set('Erreur lors du chargement des visites.');
        this.loading.set(false);
      },
    });
  }

  optimiser(): void {
    const id = this.selectedCommercialId();
    const date = this.selectedDate();
    if (!id || !date || this.visites().length === 0) return;

    this.optimizing.set(true);
    this.aiError.set(false);
    this.error.set(null);

    this.planningSvc.optimiser(id, date).subscribe({
      next: resp => {
        const sorted = [...resp.visites].sort((a, b) => {
          const oa = a.ordreTournee ?? 9999;
          const ob = b.ordreTournee ?? 9999;
          return oa - ob;
        });
        this.optimizedVisites.set(sorted);
        this.distanceTotale.set(resp.distanceTotaleKm);
        this.optimizing.set(false);
        if (this.mapReady) this.refreshMap();
      },
      error: err => {
        this.optimizing.set(false);
        if (err.status === 503 || err.status === 0) {
          this.aiError.set(true);
        } else {
          this.error.set('Erreur lors de l\'optimisation.');
        }
      },
    });
  }

  resetOptimization(): void {
    this.optimizedVisites.set(null);
    this.distanceTotale.set(null);
    this.aiError.set(false);
    this.visites.set([]);
    if (this.mapReady) this.refreshMap();
  }

  refreshMap(): void {
    if (!this.map) return;

    this.markers.forEach(m => m.remove());
    this.markers = [];
    if (this.polyline) { this.polyline.remove(); this.polyline = null; }

    const display = this.optimizedVisites() ?? this.visites();
    const isOptimized = this.optimizedVisites() !== null;
    const points: L.LatLngTuple[] = [];

    display.forEach((v, i) => {
      const cl = this.clients().find(c => c.id === v.clientId);
      if (!cl?.latitude || !cl?.longitude) return;

      const lat = cl.latitude;
      const lng = cl.longitude;
      points.push([lat, lng]);

      const icon = L.divIcon({
        html: isOptimized
          ? `<div style="width:28px;height:28px;background:#2563eb;color:#fff;border:2px solid #fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px;box-shadow:0 2px 6px rgba(0,0,0,.35)">${i + 1}</div>`
          : `<div style="width:12px;height:12px;background:#2563eb;border:2px solid #fff;border-radius:50%;box-shadow:0 1px 4px rgba(0,0,0,.3)"></div>`,
        className: '',
        iconSize: isOptimized ? [28, 28] : [12, 12],
        iconAnchor: isOptimized ? [14, 14] : [6, 6],
      });

      const marker = L.marker([lat, lng], { icon })
        .bindPopup(`<b>${v.clientNom}</b><br>${v.type}<br>${this.formatHeure(v.dateVisite)}`)
        .addTo(this.map);
      this.markers.push(marker);
    });

    if (points.length > 0) {
      if (isOptimized && points.length > 1) {
        this.polyline = L.polyline(points, { color: '#2563eb', weight: 3, dashArray: '6 4' }).addTo(this.map);
      }
      this.map.fitBounds(L.latLngBounds(points), { padding: [40, 40] });
    }
  }

  // -----------------------------------------------------------------------
  // Création de visite (commercial optionnel pour le manager)
  // -----------------------------------------------------------------------

  openVisiteForm(): void {
    this.visiteForm.reset({
      commercialId: this.selectedCommercialId(),
      clientId:   null,
      type:       TypeVisite.PROSPECTION,
      statut:     StatutVisite.PLANIFIEE,
      dateVisite: this.selectedDate(),
      heure:      '09:00',
    });
    this.showVisiteForm.set(true);
  }

  cancelVisiteForm(): void { this.showVisiteForm.set(false); }

  submitVisite(): void {
    if (this.visiteForm.invalid) { this.visiteForm.markAllAsTouched(); return; }

    this.savingVisite.set(true);
    const raw = this.visiteForm.getRawValue();
    const dt = `${raw['dateVisite']}T${raw['heure']}:00`;
    const req: VisiteRequest = {
      commercialId: raw['commercialId'] ? Number(raw['commercialId']) : null,
      clientId:     Number(raw['clientId']),
      dateVisite:   dt,
      type:         raw['type'],
      statut:       raw['statut'],
    };

    this.visiteSvc.create(req).subscribe({
      next: () => {
        this.savingVisite.set(false);
        this.showVisiteForm.set(false);
        if (this.selectedCommercialId()) this.loadVisites();
      },
      error: () => {
        this.error.set('Erreur lors de la création de la visite.');
        this.savingVisite.set(false);
      },
    });
  }

  // -----------------------------------------------------------------------
  // Affectation automatique
  // -----------------------------------------------------------------------

  openAffectationModal(): void {
    this.affectationDate.set(this.selectedDate());
    this.affectationRapport.set(null);
    this.showAffectationModal.set(true);
  }

  closeAffectationModal(): void {
    this.showAffectationModal.set(false);
    if (this.affectationRapport() && this.selectedCommercialId()) this.loadVisites();
  }

  onAffectationDateChange(event: Event): void {
    this.affectationDate.set((event.target as HTMLInputElement).value);
  }

  lancerAffectationAuto(): void {
    this.affectationLoading.set(true);
    this.planningSvc.affecterAuto({ date: this.affectationDate() }).subscribe({
      next: rapport => {
        this.affectationRapport.set(rapport);
        this.affectationLoading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors de l\'affectation automatique.');
        this.affectationLoading.set(false);
      },
    });
  }

  // -----------------------------------------------------------------------
  // Réaffectation dynamique
  // -----------------------------------------------------------------------

  openReaffectationModal(): void {
    this.reaffectationCommercialId.set(this.selectedCommercialId());
    this.reaffectationDate.set(this.selectedDate());
    this.reaffectationRapport.set(null);
    this.showReaffectationModal.set(true);
  }

  closeReaffectationModal(): void {
    this.showReaffectationModal.set(false);
    if (this.reaffectationRapport() && this.selectedCommercialId()) this.loadVisites();
  }

  onReaffectationCommercialChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    this.reaffectationCommercialId.set(id || null);
  }

  onReaffectationDateChange(event: Event): void {
    this.reaffectationDate.set((event.target as HTMLInputElement).value);
  }

  lancerReaffectation(): void {
    const cid = this.reaffectationCommercialId();
    if (!cid) return;
    this.reaffectationLoading.set(true);
    this.planningSvc.reaffecter({ commercialId: cid, dateDebut: this.reaffectationDate() }).subscribe({
      next: rapport => {
        this.reaffectationRapport.set(rapport);
        this.reaffectationLoading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors de la réaffectation.');
        this.reaffectationLoading.set(false);
      },
    });
  }

  // -----------------------------------------------------------------------
  // Helpers d'affichage
  // -----------------------------------------------------------------------

  displayVisites(): Visite[] {
    return this.optimizedVisites() ?? this.visites();
  }

  formatHeure(dt: string): string {
    try { return new Date(dt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
    catch { return '—'; }
  }

  formatDate(dt: string): string {
    try { return new Date(dt).toLocaleDateString('fr-FR'); }
    catch { return dt; }
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
}
