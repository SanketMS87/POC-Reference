import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { makeToken } from '../core/auth.service.spec';
import { LoginPage } from './login';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let http: HttpTestingController;
  let navigate: jest.SpyInstance;
  const el = () => fixture.nativeElement as HTMLElement;

  function type(selector: string, value: string): void {
    const input = el().querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  }

  const submit = () => {
    el().querySelector<HTMLButtonElement>('button.primary')!.click();
    fixture.detectChanges();
  };

  const tab = (label: string) =>
    [...el().querySelectorAll<HTMLButtonElement>('.tabs button')].find(b => b.textContent!.includes(label))!;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    navigate = jest.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('shows the sign-in form with the demo shortcut', () => {
    expect(el().querySelector('input[type="email"]')).not.toBeNull();
    expect(el().querySelector('input[type="password"]')).not.toBeNull();
    expect(el().querySelector('input[type="text"]')).toBeNull();
    expect(el().textContent).toContain('demo@rukmini.com');
  });

  it('does not call the server and shows errors when the form is invalid', () => {
    submit();
    expect(el().textContent).toContain('Enter a valid email address.');
    expect(el().textContent).toContain('Password must be at least 6 characters.');
    http.expectNone(`${environment.apiUrl}/auth/login`);
  });

  it('rejects short passwords and malformed emails', () => {
    type('input[type="email"]', 'not-an-email');
    type('input[type="password"]', '12345');
    submit();
    expect(el().textContent).toContain('Enter a valid email address.');
    expect(el().textContent).toContain('Password must be at least 6 characters.');
  });

  it('fills the demo credentials', () => {
    el().querySelector<HTMLButtonElement>('button.link')!.click();
    fixture.detectChanges();
    expect(el().querySelector<HTMLInputElement>('input[type="email"]')!.value).toBe('demo@rukmini.com');
    expect(el().querySelector<HTMLInputElement>('input[type="password"]')!.value).toBe('Demo@123');
  });

  it('signs in and navigates to the restaurants page', () => {
    type('input[type="email"]', 'asha@example.com');
    type('input[type="password"]', 'Secret@123');
    submit();
    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.body).toEqual({ email: 'asha@example.com', password: 'Secret@123' });
    expect(el().querySelector<HTMLButtonElement>('button.primary')!.disabled).toBe(true);
    req.flush({ token: makeToken(3600), user: { id: 1, name: 'Asha', email: 'asha@example.com', role: 'CUSTOMER' } });
    expect(navigate).toHaveBeenCalledWith('/restaurants');
  });

  it('shows the server message on a failed login and re-enables the button', () => {
    type('input[type="email"]', 'asha@example.com');
    type('input[type="password"]', 'wrong-pass');
    submit();
    http.expectOne(`${environment.apiUrl}/auth/login`)
      .flush({ message: 'Invalid email or password' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Invalid email or password');
    expect(el().querySelector<HTMLButtonElement>('button.primary')!.disabled).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('explains when the backend cannot be reached', () => {
    type('input[type="email"]', 'asha@example.com');
    type('input[type="password"]', 'Secret@123');
    submit();
    http.expectOne(`${environment.apiUrl}/auth/login`).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Cannot reach the server');
  });

  it('falls back to a generic message when the error has no body', () => {
    type('input[type="email"]', 'asha@example.com');
    type('input[type="password"]', 'Secret@123');
    submit();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Something went wrong');
  });

  it('switches to registration, requires a name and registers', () => {
    tab('Create account').click();
    fixture.detectChanges();
    expect(el().querySelector('input[type="text"]')).not.toBeNull();
    expect(el().textContent).not.toContain('use demo@rukmini.com');

    type('input[type="email"]', 'new@example.com');
    type('input[type="password"]', 'Secret@123');
    submit();
    expect(el().textContent).toContain('Name is required.');
    http.expectNone(`${environment.apiUrl}/auth/register`);

    type('input[type="text"]', 'New User');
    submit();
    const req = http.expectOne(`${environment.apiUrl}/auth/register`);
    expect(req.request.body).toEqual({ name: 'New User', email: 'new@example.com', password: 'Secret@123' });
    req.flush({ token: makeToken(3600), user: { id: 2, name: 'New User', email: 'new@example.com', role: 'CUSTOMER' } });
    expect(navigate).toHaveBeenCalledWith('/restaurants');
  });

  it('shows the conflict message for a duplicate registration and clears it when switching tabs', () => {
    tab('Create account').click();
    fixture.detectChanges();
    type('input[type="text"]', 'Dup');
    type('input[type="email"]', 'dup@example.com');
    type('input[type="password"]', 'Secret@123');
    submit();
    http.expectOne(`${environment.apiUrl}/auth/register`)
      .flush({ message: 'Email is already registered' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Email is already registered');

    tab('Sign in').click();
    fixture.detectChanges();
    expect(el().querySelector('.alert')).toBeNull();
  });
});
