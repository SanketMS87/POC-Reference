import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('route guards', () => {
  function run(guard: typeof authGuard, loggedIn: boolean): boolean | UrlTree {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { isLoggedIn: () => loggedIn } }]
    });
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    ) as boolean | UrlTree;
  }

  it('authGuard lets signed-in users through', () => {
    expect(run(authGuard, true)).toBe(true);
  });

  it('authGuard redirects signed-out users to /login', () => {
    const result = run(authGuard, false);
    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toBe('/login');
  });

  it('guestGuard lets guests through', () => {
    expect(run(guestGuard, false)).toBe(true);
  });

  it('guestGuard redirects signed-in users to /restaurants', () => {
    const result = run(guestGuard, true);
    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toBe('/restaurants');
  });
});
