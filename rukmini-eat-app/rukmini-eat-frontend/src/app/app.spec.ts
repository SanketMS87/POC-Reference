import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth.service';
import { CartService } from './core/cart.service';
import { routes } from './app.routes';
import { authGuard, guestGuard } from './core/auth.guard';

describe('App shell', () => {
  let fixture: ComponentFixture<App>;
  const el = () => fixture.nativeElement as HTMLElement;
  const logout = jest.fn();

  function render(loggedIn: boolean): void {
    logout.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isLoggedIn: () => loggedIn, user: () => ({ name: 'Asha' }), logout } }
      ]
    });
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  }

  it('hides the navigation for signed-out users', () => {
    render(false);
    expect(el().querySelector('header.topbar')).toBeNull();
    expect(el().querySelector('nav.tabbar')).toBeNull();
    expect(el().querySelector('router-outlet')).not.toBeNull();
    expect(el().querySelector('main')!.classList).not.toContain('page');
  });

  it('shows the header, user name and navigation links for signed-in users', () => {
    render(true);
    expect(el().querySelector('header.topbar')).not.toBeNull();
    expect(el().querySelector('.user')!.textContent).toContain('Asha');
    const links = [...el().querySelectorAll('header a')].map(a => a.getAttribute('href'));
    expect(links).toEqual(expect.arrayContaining(['/restaurants', '/orders', '/cart']));
    expect(el().querySelector('main')!.classList).toContain('page');
  });

  it('shows the cart count badge only when the cart has items', () => {
    render(true);
    expect(el().querySelector('.cart-btn .count')).toBeNull();
    const cart = TestBed.inject(CartService);
    cart.add({ id: 1, name: 'A', category: 'c', price: 10, veg: true, bestseller: false, emoji: 'x' }, { id: 1, name: 'R' });
    fixture.detectChanges();
    expect(el().querySelector('.cart-btn .count')!.textContent).toBe('1');
  });

  it('logs out from the header and the tab bar', () => {
    render(true);
    el().querySelector<HTMLButtonElement>('.user button')!.click();
    el().querySelector<HTMLButtonElement>('nav.tabbar button')!.click();
    expect(logout).toHaveBeenCalledTimes(2);
  });
});

describe('routes', () => {
  const find = (path: string) => routes.find(r => r.path === path)!;

  it('protects pages with authGuard and the login page with guestGuard', () => {
    expect(find('login').canActivate).toEqual([guestGuard]);
    for (const path of ['restaurants', 'restaurants/:id', 'cart', 'orders']) {
      expect(find(path).canActivate).toEqual([authGuard]);
    }
  });

  it('lazy-loads every page component', async () => {
    for (const path of ['login', 'restaurants', 'restaurants/:id', 'cart', 'orders']) {
      const component = await (find(path).loadComponent as () => Promise<unknown>)();
      expect(component).toBeTruthy();
    }
  });

  it('redirects the root and unknown paths to the restaurants page', () => {
    expect(find('')).toMatchObject({ pathMatch: 'full', redirectTo: 'restaurants' });
    expect(find('**').redirectTo).toBe('restaurants');
  });
});
