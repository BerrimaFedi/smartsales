import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { AuthResponse, LoginRequest, Role } from '../models/auth.model';
import { environment } from '../../../environments/environment';

const STORAGE_KEY = 'ss_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _auth = signal<AuthResponse | null>(this.restore());

  readonly isLoggedIn = computed(() => this._auth() !== null);
  readonly currentUser = this._auth.asReadonly();

  login(credentials: LoginRequest) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(tap(res => this.persist(res)));
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this._auth.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this._auth()?.token ?? null;
  }

  getRole(): Role | null {
    return this._auth()?.role ?? null;
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.getRole();
    return role !== null && roles.includes(role);
  }

  private persist(res: AuthResponse): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(res));
    this._auth.set(res);
  }

  private restore(): AuthResponse | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthResponse) : null;
    } catch {
      return null;
    }
  }
}
