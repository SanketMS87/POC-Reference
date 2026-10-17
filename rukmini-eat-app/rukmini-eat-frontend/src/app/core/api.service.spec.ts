import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests all restaurants without parameters when no filter is set', () => {
    api.restaurants({}).subscribe();
    const req = http.expectOne(r => r.url === `${environment.apiUrl}/restaurants`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('sends every active filter and sort option', () => {
    api.restaurants({ q: '  pizza  ', cuisine: 'Italian', vegOnly: true, topRated: true, fast: true, sort: 'eta' }).subscribe();
    const req = http.expectOne(r => r.url === `${environment.apiUrl}/restaurants`);
    expect(req.request.params.get('q')).toBe('pizza');
    expect(req.request.params.get('cuisine')).toBe('Italian');
    expect(req.request.params.get('vegOnly')).toBe('true');
    expect(req.request.params.get('topRated')).toBe('true');
    expect(req.request.params.get('fast')).toBe('true');
    expect(req.request.params.get('sort')).toBe('eta');
    req.flush([]);
  });

  it('omits blank, null and false filters', () => {
    api.restaurants({ q: '   ', cuisine: null, vegOnly: false, topRated: false, fast: false, sort: '' }).subscribe();
    const req = http.expectOne(r => r.url === `${environment.apiUrl}/restaurants`);
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('loads one restaurant by id', () => {
    api.restaurant(7).subscribe();
    const req = http.expectOne(`${environment.apiUrl}/restaurants/7`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('places an order and lists orders', () => {
    api.placeOrder({ items: [{ menuItemId: 3, quantity: 2 }] }).subscribe();
    const post = http.expectOne(`${environment.apiUrl}/orders`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ items: [{ menuItemId: 3, quantity: 2 }] });
    post.flush({});

    api.orders().subscribe();
    const get = http.expectOne(`${environment.apiUrl}/orders`);
    expect(get.request.method).toBe('GET');
    get.flush([]);
  });
});
