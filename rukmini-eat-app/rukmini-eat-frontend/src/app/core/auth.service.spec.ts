import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../environments/environment';
import { AuthResponse } from './models';
import { AuthService } from './auth.service';

const STORAGE_KEY = 'fl_auth';

export function makeToken(expSecondsFromNow: number | null): string {
  const payload = expSecondsFromNow === null ? {} : { exp: Math.floor(Date.now() / 1000) + expSecondsFromNow };
  return `header.${btoa(JSON.stringify(payload))}.signature`;
}

function response(token: string): AuthResponse {
  return { token, user: { id: 1, name: 'Asha', email: 'asha@example.com', role: 'CUSTOMER' } };
}

describe('AuthService', () => {
  let http: HttpTestingController;

  function create(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http?.verify());

  it('starts signed out when storage is empty', () => {
    const auth = create();
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.user()).toBeNull();
    expect(auth.token()).toBeNull();
  });

  it('logs in, stores the session and exposes the user', () => {
    const auth = create();
    const token = makeToken(3600);
    let result: AuthResponse | undefined;
    auth.login('asha@example.com', 'Secret@123').subscribe(r => (result = r));

    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'asha@example.com', password: 'Secret@123' });
    req.flush(response(token));

    expect(result?.token).toBe(token);
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.user()?.name).toBe('Asha');
    expect(auth.token()).toBe(token);
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY)!).token).toBe(token);
  });

  it('registers and stores the session', () => {
    const auth = create();
    auth.register('Asha', 'asha@example.com', 'Secret@123').subscribe();
    const req = http.expectOne(`${environment.apiUrl}/auth/register`);
    expect(req.request.body).toEqual({ name: 'Asha', email: 'asha@example.com', password: 'Secret@123' });
    req.flush(response(makeToken(3600)));
    expect(auth.isLoggedIn()).toBe(true);
    expect(localStorage.getItem(STORAGE_KEY)).not.toBeNull();
  });

  it('does not store anything when login fails', () => {
    const auth = create();
    let status = 0;
    auth.login('a@b.com', 'wrong').subscribe({ error: e => (status = e.status) });
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({ message: 'Invalid email or password' }, { status: 401, statusText: 'Unauthorized' });
    expect(status).toBe(401);
    expect(auth.isLoggedIn()).toBe(false);
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('logs out: clears state and storage, then navigates to /login', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response(makeToken(3600))));
    const auth = create();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    expect(auth.isLoggedIn()).toBe(true);

    auth.logout();

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.user()).toBeNull();
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });

  it('restores a valid session from storage', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response(makeToken(3600))));
    const auth = create();
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.user()?.email).toBe('asha@example.com');
  });

  it('discards an expired session in storage', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response(makeToken(-60))));
    const auth = create();
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.token()).toBeNull();
  });

  it('discards corrupt storage content', () => {
    localStorage.setItem(STORAGE_KEY, '{not json');
    expect(create().isLoggedIn()).toBe(false);
  });

  it('treats tokens without an exp claim or in a bad format as expired', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response(makeToken(null))));
    expect(create().isLoggedIn()).toBe(false);
    TestBed.resetTestingModule();
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response('not-a-jwt')));
    expect(create().isLoggedIn()).toBe(false);
  });

  it('reports a session as signed out once its token expires', () => {
    jest.useFakeTimers().setSystemTime(new Date('2026-01-01T00:00:00Z'));
    try {
      const auth = create();
      const exp = Math.floor(Date.now() / 1000) + 10;
      const token = `h.${btoa(JSON.stringify({ exp }))}.s`;
      auth.login('a@b.com', 'x').subscribe();
      http.expectOne(`${environment.apiUrl}/auth/login`).flush(response(token));
      expect(auth.isLoggedIn()).toBe(true);
    } finally {
      jest.useRealTimers();
    }
  });
});
