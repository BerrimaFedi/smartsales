import {
  AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit,
  ViewChild, inject, signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Calendar, EventClickArg, EventInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import frLocale from '@fullcalendar/core/locales/fr';
import { forkJoin, catchError, of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { VisiteService } from '../../core/services/visite.service';
import { CommercialService } from '../../core/services/commercial.service';
import { Visite, StatutVisite, TypeVisite } from '../../core/models/visite.model';
import { Commercial } from '../../core/models/commercial.model';
import { Role } from '../../core/models/auth.model';

const STATUT_COLORS: Record<StatutVisite, string> = {
  [StatutVisite.PLANIFIEE]: '#2563eb',
  [StatutVisite.EN_COURS]:  '#f97316',
  [StatutVisite.TERMINEE]:  '#16a34a',
  [StatutVisite.ANNULEE]:   '#9ca3af',
};

const TYPE_LABELS: Record<TypeVisite, string> = {
  [TypeVisite.PROSPECTION]: 'Prospection',
  [TypeVisite.RELANCE]:     'Relance',
  [TypeVisite.NEGOCIATION]: 'Négociation',
};

const STATUT_LABELS: Record<StatutVisite, string> = {
  [StatutVisite.PLANIFIEE]: 'Planifiée',
  [StatutVisite.EN_COURS]:  'En cours',
  [StatutVisite.TERMINEE]:  'Terminée',
  [StatutVisite.ANNULEE]:   'Annulée',
};

@Component({
  selector: 'app-calendrier',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './calendrier.html',
  styleUrl: './calendrier.scss',
})
export class Calendrier implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('calendarEl') calendarEl!: ElementRef<HTMLDivElement>;

  private readonly auth = inject(AuthService);
  private readonly visiteSvc = inject(VisiteService);
  private readonly commercialSvc = inject(CommercialService);
  private readonly zone = inject(NgZone);

  readonly isManager = this.auth.hasRole(Role.ADMIN, Role.MANAGER);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly allVisites = signal<Visite[]>([]);
  readonly commerciaux = signal<Commercial[]>([]);
  readonly filterCommercialId = signal<number | null>(null);
  readonly selectedVisite = signal<Visite | null>(null);

  readonly STATUT_COLORS = STATUT_COLORS;
  readonly STATUT_LABELS = STATUT_LABELS;
  readonly TYPE_LABELS = TYPE_LABELS;
  readonly legendItems = Object.entries(STATUT_COLORS) as [StatutVisite, string][];

  private calendar!: Calendar;
  private calendarReady = false;

  ngOnInit(): void {
    if (this.isManager) {
      forkJoin({
        visites:     this.visiteSvc.getAll().pipe(catchError(() => of([] as Visite[]))),
        commerciaux: this.commercialSvc.getAll().pipe(catchError(() => of([] as Commercial[]))),
      }).subscribe({
        next: ({ visites, commerciaux }) => {
          this.allVisites.set(visites);
          this.commerciaux.set(commerciaux);
          this.loading.set(false);
          if (this.calendarReady) this.refreshEvents();
        },
        error: () => {
          this.error.set('Impossible de charger les données.');
          this.loading.set(false);
        },
      });
    } else {
      this.visiteSvc.getAll().subscribe({
        next: visites => {
          this.allVisites.set(visites);
          this.loading.set(false);
          if (this.calendarReady) this.refreshEvents();
        },
        error: () => {
          this.error.set('Impossible de charger les visites.');
          this.loading.set(false);
        },
      });
    }
  }

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      this.calendar = new Calendar(this.calendarEl.nativeElement, {
        plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
        initialView: 'dayGridMonth',
        locale: frLocale,
        headerToolbar: {
          left:   'prev,next today',
          center: 'title',
          right:  'dayGridMonth,timeGridWeek,timeGridDay',
        },
        buttonText: {
          today: "Aujourd'hui",
          month: 'Mois',
          week:  'Semaine',
          day:   'Jour',
        },
        height: 'auto',
        eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
        eventClick: (arg: EventClickArg) => {
          const visite = arg.event.extendedProps['visite'] as Visite;
          this.zone.run(() => this.selectedVisite.set(visite));
        },
      });
      this.calendar.render();
    });

    this.calendarReady = true;
    if (!this.loading()) this.refreshEvents();
  }

  ngOnDestroy(): void {
    if (this.calendar) this.calendar.destroy();
  }

  onCommercialFilter(event: Event): void {
    const v = (event.target as HTMLSelectElement).value;
    this.filterCommercialId.set(v ? Number(v) : null);
    this.refreshEvents();
  }

  closeDetail(): void { this.selectedVisite.set(null); }

  formatDateHeure(dt: string): string {
    try {
      const d = new Date(dt);
      return (
        d.toLocaleDateString('fr-FR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) +
        ' à ' +
        d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
      );
    } catch { return dt; }
  }

  formatMontant(m: number): string {
    if (!m || m === 0) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(m) + ' DT';
  }

  private buildEvents(): EventInput[] {
    const filterId = this.filterCommercialId();
    const visites = filterId !== null
      ? this.allVisites().filter(v => v.commercialId === filterId)
      : this.allVisites();

    return visites.map(v => ({
      id:              v.id.toString(),
      title:           `${v.clientNom} – ${TYPE_LABELS[v.type]}`,
      start:           v.dateVisite,
      backgroundColor: STATUT_COLORS[v.statut] ?? '#6b7280',
      borderColor:     STATUT_COLORS[v.statut] ?? '#6b7280',
      textColor:       '#ffffff',
      extendedProps:   { visite: v },
    }));
  }

  private refreshEvents(): void {
    if (!this.calendar) return;
    this.calendar.removeAllEventSources();
    this.calendar.addEventSource(this.buildEvents());
  }
}
