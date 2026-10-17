import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

type Mode = 'login' | 'register';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  template: `
    <div class="auth-page">
      <section class="auth-hero">
        <img class="auth-logo" src="logo.svg" alt="Rukmini Eat App">
        <p class="tagline">Hungry? Your favourite restaurants, delivered fast.</p>
        <ul>
          <li>🍛 Hundreds of dishes from top-rated kitchens</li>
          <li>⚡ Quick delivery with live order history</li>
          <li>🏷️ Transparent pricing, free delivery over ₹399</li>
        </ul>
      </section>

      <section class="auth-card">
        <div class="tabs">
          <button type="button" [class.on]="mode() === 'login'" (click)="setMode('login')">Sign in</button>
          <button type="button" [class.on]="mode() === 'register'" (click)="setMode('register')">Create account</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          @if (mode() === 'register') {
            <label>
              Full name
              <input type="text" formControlName="name" autocomplete="name" placeholder="Your name">
            </label>
            @if (invalid('name')) { <small class="err">Name is required.</small> }
          }

          <label>
            Email
            <input type="email" formControlName="email" autocomplete="email" placeholder="you@example.com">
          </label>
          @if (invalid('email')) { <small class="err">Enter a valid email address.</small> }

          <label>
            Password
            <input type="password" formControlName="password"
                   [attr.autocomplete]="mode() === 'login' ? 'current-password' : 'new-password'"
                   placeholder="At least 6 characters">
          </label>
          @if (invalid('password')) { <small class="err">Password must be at least 6 characters.</small> }

          @if (error()) { <div class="alert">{{ error() }}</div> }

          <button class="primary" type="submit" [disabled]="loading()">
            {{ loading() ? 'Please wait…' : mode() === 'login' ? 'Sign in' : 'Create account' }}
          </button>
        </form>

        @if (mode() === 'login') {
          <p class="hint">
            Demo account:
            <button type="button" class="link" (click)="fillDemo()">use demo@rukmini.com</button>
          </p>
        }
      </section>
    </div>
  `
})
export class LoginPage {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly mode = signal<Mode>('login');
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.fb.group({
    name: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  protected setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set('');
    const name = this.form.controls.name;
    name.setValidators(mode === 'register' ? [Validators.required] : []);
    name.updateValueAndValidity();
  }

  protected fillDemo(): void {
    this.form.patchValue({ email: 'demo@rukmini.com', password: 'Demo@123' });
  }

  protected invalid(field: 'name' | 'email' | 'password'): boolean {
    const c = this.form.controls[field];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set('');

    const request$ = this.mode() === 'login'
      ? this.auth.login(email, password)
      : this.auth.register(name, email, password);

    request$.subscribe({
      next: () => this.router.navigateByUrl('/restaurants'),
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(e.status === 0 ? 'Cannot reach the server. Is the backend running?' : e.error?.message ?? 'Something went wrong');
      }
    });
  }
}
