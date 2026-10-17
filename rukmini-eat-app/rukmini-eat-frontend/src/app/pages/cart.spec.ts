import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { CartService } from '../core/cart.service';
import { MenuItem } from '../core/models';
import { CartPage } from './cart';

const dish = (id: number, name: string, price: number, veg = true): MenuItem => ({
  id, name, category: 'Mains', price, veg, bestseller: false, emoji: '🍽️'
});
const pizza = { id: 1, name: 'Pizza Piazza' };

describe('CartPage', () => {
  let fixture: ComponentFixture<CartPage>;
  let http: HttpTestingController;
  let cart: CartService;
  let navigate: jest.SpyInstance;
  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    cart = TestBed.inject(CartService);
    navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  function render(): void {
    fixture = TestBed.createComponent(CartPage);
    fixture.detectChanges();
  }

  const placeButton = () => el().querySelector<HTMLButtonElement>('button.primary')!;

  it('shows an empty state with a link back to the restaurants', () => {
    render();
    expect(el().textContent).toContain('Your cart is empty.');
    expect(el().querySelector('a.primary')!.getAttribute('href')).toBe('/restaurants');
  });

  it('lists lines and the bill with a delivery nudge below the threshold', () => {
    cart.add(dish(1, 'Margherita', 199), pizza);
    render();
    expect(el().querySelector('h2')!.textContent).toBe('Pizza Piazza');
    expect(el().querySelectorAll('.panel .line').length).toBeGreaterThan(0);
    expect(el().textContent).toContain('more for free delivery');
    expect(el().textContent).toContain('199.00');
    expect(el().textContent).toContain('253.95');
  });

  it('celebrates free delivery at or above the threshold', () => {
    cart.add(dish(1, 'Feast', 450), pizza);
    render();
    expect(el().textContent).toContain('You unlocked free delivery');
    expect(el().textContent).toContain('FREE');
  });

  it('changes quantities and clears the cart', () => {
    cart.add(dish(1, 'Margherita', 199), pizza);
    render();
    el().querySelector<HTMLButtonElement>('[aria-label="Add one"]')!.click();
    fixture.detectChanges();
    expect(cart.qty(1)).toBe(2);
    el().querySelector<HTMLButtonElement>('[aria-label="Remove one"]')!.click();
    fixture.detectChanges();
    expect(cart.qty(1)).toBe(1);
    el().querySelector<HTMLButtonElement>('button.link')!.click();
    fixture.detectChanges();
    expect(cart.count()).toBe(0);
    expect(el().textContent).toContain('Your cart is empty.');
  });

  it('places the order with item ids and quantities, clears the cart and opens the orders page', () => {
    cart.add(dish(1, 'Margherita', 199), pizza);
    cart.add(dish(1, 'Margherita', 199), pizza);
    cart.add(dish(2, 'Cola', 60), pizza);
    render();
    placeButton().click();
    fixture.detectChanges();
    expect(placeButton().disabled).toBe(true);

    const req = http.expectOne(`${environment.apiUrl}/orders`);
    expect(req.request.body).toEqual({ items: [{ menuItemId: 1, quantity: 2 }, { menuItemId: 2, quantity: 1 }] });
    req.flush({ id: 77 });

    expect(cart.count()).toBe(0);
    expect(navigate).toHaveBeenCalledWith(['/orders'], { queryParams: { placed: 77 } });
  });

  it('shows the server message and keeps the cart when ordering fails', () => {
    cart.add(dish(1, 'Margherita', 199), pizza);
    render();
    placeButton().click();
    http.expectOne(`${environment.apiUrl}/orders`)
      .flush({ message: 'Some menu items do not exist' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Some menu items do not exist');
    expect(placeButton().disabled).toBe(false);
    expect(cart.count()).toBe(1);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('uses a generic message when the error has no body', () => {
    cart.add(dish(1, 'Margherita', 199), pizza);
    render();
    placeButton().click();
    http.expectOne(`${environment.apiUrl}/orders`).flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Could not place the order.');
  });
});
