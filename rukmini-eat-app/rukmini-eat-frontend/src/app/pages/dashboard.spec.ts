import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { RestaurantSummary } from '../core/models';
import { DashboardPage } from './dashboard';

const restaurant = (id: number, name: string, eta: number, pureVeg = false): RestaurantSummary => ({
  id, name, cuisines: ['Pizza', 'Italian'], rating: 4.4, etaMinutes: eta, costForTwo: 450, pureVeg, emoji: '🍕',
  description: 'Test'
});

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;
  const url = `${environment.apiUrl}/restaurants`;

  function expectRequest() {
    return http.expectOne(r => r.url === url);
  }

  function settle(list: RestaurantSummary[] = []): void {
    tick(200);
    expectRequest().flush(list);
    fixture.detectChanges();
  }

  const chip = (label: string) =>
    [...el().querySelectorAll<HTMLButtonElement>('.chip')].find(b => b.textContent!.includes(label))!;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardPage);
  });

  afterEach(() => http.verify());

  it('loads restaurants without filters and shows the stats', fakeAsync(() => {
    fixture.detectChanges();
    expect(el().textContent).toContain('Loading restaurants');
    tick(200);
    const req = expectRequest();
    expect(req.request.params.keys()).toEqual(['sort']);
    expect(req.request.params.get('sort')).toBe('relevance');
    req.flush([restaurant(1, 'Pizza Piazza', 25), restaurant(2, 'Green Bowl', 24, true), restaurant(3, 'Burger Barn', 22)]);
    fixture.detectChanges();

    expect(el().querySelectorAll('.rcard').length).toBe(3);
    expect(el().textContent).toContain('Pizza Piazza');
    const stats = [...el().querySelectorAll('.stat b')].map(b => b.textContent);
    expect(stats).toEqual(['3', '24', '1']);
    expect(el().querySelectorAll('.tag.green').length).toBe(1);
    expect(el().querySelector<HTMLAnchorElement>('.rcard')!.getAttribute('href')).toBe('/restaurants/1');
  }));

  it('shows a friendly empty state', fakeAsync(() => {
    fixture.detectChanges();
    settle([]);
    expect(el().textContent).toContain('No restaurants match your search.');
    expect(el().querySelector('.stat b')!.textContent).toBe('0');
    expect(el().querySelectorAll('.stat b')[1].textContent).toBe('0');
  }));

  it('shows an error when the backend is unavailable', fakeAsync(() => {
    fixture.detectChanges();
    tick(200);
    expectRequest().error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(el().querySelector('.alert')!.textContent).toContain('Could not load restaurants');
    expect(el().textContent).not.toContain('No restaurants match');
  }));

  it('applies the filter chips', fakeAsync(() => {
    fixture.detectChanges();
    settle();

    chip('Pure veg').click();
    tick(200);
    let req = expectRequest();
    expect(req.request.params.get('vegOnly')).toBe('true');
    req.flush([]);
    fixture.detectChanges();
    expect(chip('Pure veg').classList).toContain('on');

    chip('Rating').click();
    chip('Under 25').click();
    tick(200);
    req = expectRequest();
    expect(req.request.params.get('vegOnly')).toBe('true');
    expect(req.request.params.get('topRated')).toBe('true');
    expect(req.request.params.get('fast')).toBe('true');
    req.flush([]);

    chip('Pure veg').click();
    tick(200);
    req = expectRequest();
    expect(req.request.params.has('vegOnly')).toBe(false);
    req.flush([]);
  }));

  it('selects and deselects a cuisine', fakeAsync(() => {
    fixture.detectChanges();
    settle();
    const biryani = () =>
      [...el().querySelectorAll<HTMLButtonElement>('.cuisine')].find(b => b.textContent!.includes('Biryani'))!;

    biryani().click();
    tick(200);
    let req = expectRequest();
    expect(req.request.params.get('cuisine')).toBe('Biryani');
    req.flush([]);
    fixture.detectChanges();
    expect(biryani().classList).toContain('on');

    biryani().click();
    tick(200);
    req = expectRequest();
    expect(req.request.params.has('cuisine')).toBe(false);
    req.flush([]);
  }));

  it('searches (debounced) and sorts', fakeAsync(() => {
    fixture.detectChanges();
    settle();

    const search = el().querySelector<HTMLInputElement>('input[type="search"]')!;
    search.value = 'piz';
    search.dispatchEvent(new Event('input'));
    tick(100);
    search.value = 'pizza';
    search.dispatchEvent(new Event('input'));
    tick(200);
    let req = expectRequest();
    expect(req.request.params.get('q')).toBe('pizza');
    req.flush([]);

    const select = el().querySelector<HTMLSelectElement>('select')!;
    select.value = 'costLow';
    select.dispatchEvent(new Event('change'));
    tick(200);
    req = expectRequest();
    expect(req.request.params.get('sort')).toBe('costLow');
    expect(req.request.params.get('q')).toBe('pizza');
    req.flush([]);
  }));

  it('greets according to the time of day', fakeAsync(() => {
    const hours = jest.spyOn(Date.prototype, 'getHours');
    try {
      hours.mockReturnValue(8);
      fixture.detectChanges();
      expect(el().querySelector('h1')!.textContent).toContain('Good morning');
      hours.mockReturnValue(12);
      fixture.detectChanges();
      expect(el().querySelector('h1')!.textContent).toContain('Good afternoon');
      hours.mockReturnValue(16);
      fixture.detectChanges();
      expect(el().querySelector('h1')!.textContent).toContain('Good afternoon');
      hours.mockReturnValue(17);
      fixture.detectChanges();
      expect(el().querySelector('h1')!.textContent).toContain('Good evening');
    } finally {
      hours.mockRestore();
    }
    tick(200);
    expectRequest().flush([]);
  }));
});
