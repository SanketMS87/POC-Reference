import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from './models';

const STORAGE_KEY = 'fl_auth';

function isExpired(token: string): boolean {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const { exp } = JSON.parse(atob(payload)) as { exp?: number };
    return !exp || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly state = signal<AuthResponse | null>(this.restore());

  readonly user = computed(() => this.state()?.user ?? null);
  readonly token = computed(() => this.state()?.token ?? null);
  readonly isLoggedIn = computed(() => {
    const t = this.token();
    return !!t && !isExpired(t);
  });

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(tap(res => this.persist(res)));
  }

  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, { name, email, password })
      .pipe(tap(res => this.persist(res)));
  }

  logout(): void {
    this.state.set(null);
    localStorage.removeItem(STORAGE_KEY);
    this.router.navigateByUrl('/login');
  }

  private persist(res: AuthResponse): void {
    this.state.set(res);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
  }

  private restore(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      const res = raw ? (JSON.parse(raw) as AuthResponse) : null;
      return res && !isExpired(res.token) ? res : null;
    } catch {
      return null;
    }
  }
}
