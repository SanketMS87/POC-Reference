import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { Subject, catchError, debounceTime, of, startWith, switchMap, tap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { RestaurantSummary } from '../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <section class="hero">
      <div>
        <h1>{{ greeting() }}, Rukmini EAT! 👋</h1>
        <p class="muted">What are you craving today?</p>
      </div>
      <div class="stats">
        <div class="stat"><b>{{ restaurants().length }}</b><span>Restaurants</span></div>
        <div class="stat"><b>{{ avgEta() }}</b><span>Avg mins</span></div>
        <div class="stat"><b>{{ pureVegCount() }}</b><span>Pure veg</span></div>
      </div>
    </section>

    <div class="searchbar">
      <input type="search" placeholder="Search restaurants, cuisines or dishes" aria-label="Search"
             [value]="q()" (input)="onSearch($event)">
    </div>

    <h2>What's on your mind?</h2>
    <div class="cuisine-strip">
      @for (c of cuisines; track c.name) {
        <button type="button" class="cuisine" [class.on]="cuisine() === c.name" (click)="toggleCuisine(c.name)">
          <span class="ico">{{ c.emoji }}</span>{{ c.name }}
        </button>
      }
    </div>

    <div class="filters">
      <button type="button" class="chip" [class.on]="vegOnly()" (click)="toggle(vegOnly)">🟢 Pure veg</button>
      <button type="button" class="chip" [class.on]="topRated()" (click)="toggle(topRated)">★ Rating 4.3+</button>
      <button type="button" class="chip" [class.on]="fast()" (click)="toggle(fast)">⚡ Under 25 mins</button>
      <select aria-label="Sort" [value]="sort()" (change)="onSort($event)">
        <option value="relevance">Sort: Relevance</option>
        <option value="rating">Rating</option>
        <option value="eta">Delivery time</option>
        <option value="costLow">Cost: low to high</option>
        <option value="costHigh">Cost: high to low</option>
      </select>
    </div>

    @if (error()) { <div class="alert">{{ error() }}</div> }

    @if (loading()) {
      <p class="muted">Loading restaurants…</p>
    } @else if (restaurants().length === 0 && !error()) {
      <div class="empty">No restaurants match your search.</div>
    } @else {
      <div class="grid">
        @for (r of restaurants(); track r.id) {
          <a class="rcard" [routerLink]="['/restaurants', r.id]">
            <div class="banner">{{ r.emoji }}</div>
            <div class="rbody">
              <div class="row"><h3>{{ r.name }}</h3><span class="rating">★ {{ r.rating }}</span></div>
              <p class="muted">{{ r.cuisines.join(', ') }}</p>
              <div class="row muted">
                <span>⏱ {{ r.etaMinutes }} mins</span>
                <span>{{ r.costForTwo | currency: 'INR' : 'symbol-narrow' : '1.0-0' }} for two</span>
              </div>
              @if (r.pureVeg) { <span class="tag green">Pure veg</span> }
            </div>
          </a>
        }
      </div>
    }
  `
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refresh$ = new Subject<void>();

  protected readonly cuisines = [
    { name: 'Biryani', emoji: '🍛' }, { name: 'Pizza', emoji: '🍕' }, { name: 'Burgers', emoji: '🍔' },
    { name: 'North Indian', emoji: '🥘' }, { name: 'Chinese', emoji: '🥡' }, { name: 'South Indian', emoji: '🥞' },
    { name: 'Healthy', emoji: '🥗' }, { name: 'Desserts', emoji: '🍰' }
  ];

  protected readonly q = signal('');
  protected readonly cuisine = signal<string | null>(null);
  protected readonly vegOnly = signal(false);
  protected readonly topRated = signal(false);
  protected readonly fast = signal(false);
  protected readonly sort = signal('relevance');

  protected readonly restaurants = signal<RestaurantSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected readonly avgEta = computed(() => {
    const list = this.restaurants();
    return list.length ? Math.round(list.reduce((s, r) => s + r.etaMinutes, 0) / list.length) : 0;
  });
  protected readonly pureVegCount = computed(() => this.restaurants().filter(r => r.pureVeg).length);

  protected greeting(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
  }

  ngOnInit(): void {
    this.refresh$
      .pipe(
        startWith(undefined),
        debounceTime(200),
        tap(() => {
          this.loading.set(true);
          this.error.set('');
        }),
        switchMap(() =>
          this.api
            .restaurants({
              q: this.q(),
              cuisine: this.cuisine(),
              vegOnly: this.vegOnly(),
              topRated: this.topRated(),
              fast: this.fast(),
              sort: this.sort()
            })
            .pipe(
              catchError(() => {
                this.error.set('Could not load restaurants. Is the backend running?');
                return of([] as RestaurantSummary[]);
              })
            )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(list => {
        this.restaurants.set(list);
        this.loading.set(false);
      });
  }

  protected onSearch(e: Event): void {
    this.q.set((e.target as HTMLInputElement).value);
    this.refresh$.next();
  }

  protected onSort(e: Event): void {
    this.sort.set((e.target as HTMLSelectElement).value);
    this.refresh$.next();
  }

  protected toggle(flag: { update: (fn: (v: boolean) => boolean) => void }): void {
    flag.update(v => !v);
    this.refresh$.next();
  }

  protected toggleCuisine(name: string): void {
    this.cuisine.update(c => (c === name ? null : name));
    this.refresh$.next();
  }
}
