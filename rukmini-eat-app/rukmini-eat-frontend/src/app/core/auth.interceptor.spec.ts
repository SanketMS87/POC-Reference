import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let token: string | null;
  const logout = jest.fn();

  beforeEach(() => {
    token = 'abc.def.ghi';
    logout.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => token, logout } }
      ]
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds the bearer token to API calls', () => {
    http.get(`${environment.apiUrl}/restaurants`).subscribe();
    const req = controller.expectOne(`${environment.apiUrl}/restaurants`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    req.flush([]);
  });

  it('does not send the token to login or register', () => {
    for (const path of ['/auth/login', '/auth/register']) {
      http.post(`${environment.apiUrl}${path}`, {}).subscribe();
      const req = controller.expectOne(`${environment.apiUrl}${path}`);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush({});
    }
  });

  it('does not leak the token to other origins', () => {
    http.get('https://example.org/data').subscribe();
    const req = controller.expectOne('https://example.org/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('sends no header when signed out', () => {
    token = null;
    http.get(`${environment.apiUrl}/orders`).subscribe();
    const req = controller.expectOne(`${environment.apiUrl}/orders`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('logs out and rethrows on a 401 from the API', () => {
    let status = 0;
    http.get(`${environment.apiUrl}/orders`).subscribe({ error: e => (status = e.status) });
    controller.expectOne(`${environment.apiUrl}/orders`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(status).toBe(401);
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('does not log out on a failed login (401) or on other errors', () => {
    http.post(`${environment.apiUrl}/auth/login`, {}).subscribe({ error: () => undefined });
    controller.expectOne(`${environment.apiUrl}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.get(`${environment.apiUrl}/orders`).subscribe({ error: () => undefined });
    controller.expectOne(`${environment.apiUrl}/orders`).flush({}, { status: 500, statusText: 'Server Error' });
    http.get('https://example.org/x').subscribe({ error: () => undefined });
    controller.expectOne('https://example.org/x').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logout).not.toHaveBeenCalled();
  });
});
