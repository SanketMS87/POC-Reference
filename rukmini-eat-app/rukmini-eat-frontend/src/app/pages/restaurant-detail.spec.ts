import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { CartService } from '../core/cart.service';
import { MenuItem, RestaurantDetail } from '../core/models';
import { RestaurantDetailPage } from './restaurant-detail';

const dish = (id: number, name: string, category: string, price: number, veg: boolean, bestseller = false): MenuItem => ({
  id, name, category, price, veg, bestseller, emoji: '🍽️'
});

const detail: RestaurantDetail = {
  id: 5, name: 'Pizza Piazza', cuisines: ['Pizza', 'Italian'], rating: 4.3, etaMinutes: 25, costForTwo: 450,
  pureVeg: false, emoji: '🍕', description: 'Wood-fired pizzas',
  menu: [
    dish(1, 'Margherita', 'Pizzas', 199, true, true),
    dish(2, 'Pepperoni', 'Pizzas', 349, false),
    dish(3, 'Cola', 'Drinks', 60, true)
  ]
};

describe('RestaurantDetailPage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<RestaurantDetailPage>;
  let navigate: jest.SpyInstance;
  const el = () => fixture.nativeElement as HTMLElement;

  function setup(id: string | null): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    navigate = jest.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(RestaurantDetailPage);
    fixture.detectChanges();
  }

  afterEach(() => {
    http.verify();
    jest.restoreAllMocks();
  });

  const loaded = () => {
    http.expectOne(`${environment.apiUrl}/restaurants/5`).flush(detail);
    fixture.detectChanges();
  };
  const addButtons = () => [...el().querySelectorAll<HTMLButtonElement>('.add-btn')];

  it('shows a loading message, then the menu grouped by category', () => {
    setup('5');
    expect(el().textContent).toContain('Loading menu');
    loaded();
    expect(el().querySelector('h1')!.textContent).toContain('Pizza Piazza');
    expect([...el().querySelectorAll('.cat h2')].map(h => h.textContent)).toEqual(['Pizzas', 'Drinks']);
    expect(el().querySelectorAll('.dish').length).toBe(3);
    expect(el().textContent).toContain('Bestseller');
    expect(el().querySelectorAll('.vdot.nonveg').length).toBe(1);
  });

  it('filters to vegetarian dishes and hides empty categories', () => {
    setup('5');
    loaded();
    const toggle = el().querySelector<HTMLInputElement>('.switch input')!;
    toggle.click();
    fixture.detectChanges();
    expect(el().querySelectorAll('.dish').length).toBe(2);
    expect(el().textContent).not.toContain('Pepperoni');
    toggle.click();
    fixture.detectChanges();
    expect(el().querySelectorAll('.dish').length).toBe(3);
  });

  it('adds dishes to the cart, shows steppers and the cart bar', () => {
    setup('5');
    loaded();
    const cart = TestBed.inject(CartService);
    expect(el().querySelector('.cartbar')).toBeNull();

    addButtons()[0].click();
    fixture.detectChanges();
    expect(cart.qty(1)).toBe(1);
    expect(cart.restaurant()).toEqual({ id: 5, name: 'Pizza Piazza' });
    expect(el().querySelector('.cartbar')!.textContent).toContain('1 item(s)');

    el().querySelector<HTMLButtonElement>('[aria-label="Add one"]')!.click();
    fixture.detectChanges();
    expect(cart.qty(1)).toBe(2);
    el().querySelector<HTMLButtonElement>('[aria-label="Remove one"]')!.click();
    el().querySelector<HTMLButtonElement>('[aria-label="Remove one"]')!.click();
    fixture.detectChanges();
    expect(cart.count()).toBe(0);
    expect(addButtons().length).toBe(3);
  });

  it('shows a message when the restaurant does not exist', () => {
    setup('5');
    http.expectOne(`${environment.apiUrl}/restaurants/5`).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Restaurant not found.');
  });

  it('redirects to the list for a non-numeric id without calling the API', () => {
    setup('abc');
    expect(navigate).toHaveBeenCalledWith('/restaurants');
    http.expectNone(r => r.url.includes('/restaurants/'));
  });

  it('redirects when no id is present', () => {
    setup('1.5');
    expect(navigate).toHaveBeenCalledWith('/restaurants');
  });
});
