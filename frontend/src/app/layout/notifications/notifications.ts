import {
  Component,
  HostListener,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationItem } from '../../core/models/notification.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications implements OnInit {
  private readonly svc = inject(NotificationService);

  readonly isOpen        = signal(false);
  readonly count         = signal(0);
  readonly items         = signal<NotificationItem[]>([]);
  readonly loading       = signal(false);
  readonly erreur        = signal<string | null>(null);

  ngOnInit(): void {
    this.charger();
  }

  toggle(): void {
    const ouvert = !this.isOpen();
    this.isOpen.set(ouvert);
    // Recharge à chaque ouverture du panneau
    if (ouvert) this.charger();
  }

  /** Ferme le dropdown si le clic est hors du composant. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const el = (event.target as HTMLElement).closest('app-notifications');
    if (!el) this.isOpen.set(false);
  }

  /** Retourne l'icône unicode correspondant au type de notification. */
  icone(type: string): string {
    switch (type) {
      case 'RETARD':     return '⚠️';
      case 'RAPPEL':     return '🔔';
      case 'SUGGESTION': return '💡';
      default:           return '📌';
    }
  }

  private charger(): void {
    this.loading.set(true);
    this.erreur.set(null);

    this.svc.getNotifications().subscribe({
      next: res => {
        this.count.set(res.count);
        this.items.set(res.notifications);
        this.loading.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les notifications.');
        this.loading.set(false);
      },
    });
  }
}
