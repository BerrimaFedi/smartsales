import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Role } from '../../core/models/auth.model';
import { Chatbot } from '../chatbot/chatbot';
import { Notifications } from '../notifications/notifications';

interface NavItem {
  label: string;
  route: string;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Tableau de bord',  route: '/dashboard',    roles: [Role.ADMIN, Role.MANAGER, Role.COMMERCIAL] },
  { label: 'Commerciaux',      route: '/commerciaux',  roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Clients',          route: '/clients',      roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Zones',            route: '/zones',        roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Compétences',     route: '/competences',  roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Planning',         route: '/planning',       roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Calendrier',       route: '/calendrier',     roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Toutes les visites', route: '/toutes-visites', roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Performances',     route: '/performances',   roles: [Role.ADMIN, Role.MANAGER] },
  { label: 'Mon planning',     route: '/planning',     roles: [Role.COMMERCIAL] },
  { label: 'Calendrier',      route: '/calendrier',   roles: [Role.COMMERCIAL] },
  { label: 'Mes visites',      route: '/mes-visites',  roles: [Role.COMMERCIAL] },
  { label: 'Mes performances', route: '/performances', roles: [Role.COMMERCIAL] },
];

const ROLE_LABELS: Record<Role, string> = {
  [Role.ADMIN]:      'Administrateur',
  [Role.MANAGER]:    'Manager',
  [Role.COMMERCIAL]: 'Commercial',
};

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Chatbot, Notifications],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);

  readonly currentUser = this.auth.currentUser;

  readonly visibleNavItems = computed(() => {
    const role = this.auth.getRole();
    return role ? NAV_ITEMS.filter(i => i.roles.includes(role)) : [];
  });

  readonly roleLabel = computed(() => {
    const role = this.auth.getRole();
    return role ? ROLE_LABELS[role] : '';
  });

  readonly userInitial = computed(() =>
    (this.currentUser()?.username ?? '?').charAt(0).toUpperCase(),
  );

  logout(): void { this.auth.logout(); }
}
