import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { Order } from '../core/models';
import { OrdersPage } from './orders';

const order = (id: number, status: string): Order => ({
  id, restaurantName: 'Pizza Piazza', status,
  items: [{ name: 'Margherita', price: 199, quantity: 2 }],
  subtotal: 398, deliveryFee: 40, tax: 19.9, platformFee: 5, total: 462.9, createdAt: '2026-10-17T06:30:00Z'
});

describe('OrdersPage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<OrdersPage>;
  const el = () => fixture.nativeElement as HTMLElement;

  function setup(placed: string | null = null): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(placed ? { placed } : {}) } } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrdersPage);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const respond = (list: Order[]) => {
    http.expectOne(`${environment.apiUrl}/orders`).flush(list);
    fixture.detectChanges();
  };

  it('shows a loading message until orders arrive', () => {
    setup();
    expect(el().textContent).toContain('Loading orders');
    respond([]);
    expect(el().textContent).not.toContain('Loading orders');
  });

  it('shows the empty state with a call to action', () => {
    setup();
    respond([]);
    expect(el().textContent).toContain('No orders yet.');
    expect(el().querySelector('a.primary')!.getAttribute('href')).toBe('/restaurants');
  });

  it('lists orders with friendly status labels and totals', () => {
    setup();
    respond([order(2, 'PREPARING'), order(1, 'PLACED'), order(0, 'OUT_FOR_DELIVERY'), order(9, 'DELIVERED')]);
    const articles = el().querySelectorAll('article.order');
    expect(articles.length).toBe(4);
    expect([...el().querySelectorAll('.tag.green')].map(t => t.textContent))
      .toEqual(['Preparing', 'Order placed', 'Out for delivery', 'Delivered']);
    expect(articles[0].textContent).toContain('Pizza Piazza');
    expect(articles[0].textContent).toContain('2 × Margherita');
    expect(articles[0].textContent).toContain('462.90');
  });

  it('shows unknown statuses as they are', () => {
    setup();
    respond([order(1, 'MYSTERY')]);
    expect(el().querySelector('.tag.green')!.textContent).toBe('MYSTERY');
  });

  it('announces a freshly placed order', () => {
    setup('42');
    respond([order(42, 'PLACED')]);
    expect(el().querySelector('.alert.success')!.textContent).toContain('Order #42 placed successfully!');
  });

  it('does not show the banner without the query parameter', () => {
    setup();
    respond([order(1, 'PLACED')]);
    expect(el().querySelector('.alert.success')).toBeNull();
  });

  it('shows an error when the orders cannot be loaded', () => {
    setup();
    http.expectOne(`${environment.apiUrl}/orders`).flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Could not load your orders.');
    expect(el().textContent).not.toContain('No orders yet.');
    expect(el().textContent).not.toContain('Loading orders');
  });
});
