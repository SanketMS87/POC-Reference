import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';
import { MenuItem, RestaurantDetail } from '../core/models';

@Component({
  selector: 'app-restaurant-detail',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <a class="back" routerLink="/restaurants">← Back to restaurants</a>

    @if (error()) {
      <div class="alert">{{ error() }}</div>
    } @else if (restaurant(); as r) {
      <div class="rhead">
        <div>
          <h1>{{ r.emoji }} {{ r.name }}</h1>
          <p class="muted">{{ r.cuisines.join(', ') }} · ⏱ {{ r.etaMinutes }} mins ·
            {{ r.costForTwo | currency: 'INR' : 'symbol-narrow' : '1.0-0' }} for two</p>
          <p class="muted">{{ r.description }}</p>
        </div>
        <div>
          <span class="rating">★ {{ r.rating }}</span>
          <label class="switch">
            <input type="checkbox" [checked]="vegOnly()" (change)="vegOnly.set(!vegOnly())"> Veg only
          </label>
        </div>
      </div>

      @for (cat of categories(); track cat.name) {
        <section class="cat">
          <h2>{{ cat.name }}</h2>
          @for (item of cat.items; track item.id) {
            <div class="dish">
              <div>
                <span class="vdot" [class.veg]="item.veg" [class.nonveg]="!item.veg" [title]="item.veg ? 'Veg' : 'Non-veg'"></span>
                @if (item.bestseller) { <span class="tag best">★ Bestseller</span> }
                <h4>{{ item.name }}</h4>
                <p class="price">{{ item.price | currency: 'INR' : 'symbol-narrow' : '1.0-0' }}</p>
              </div>
              <div class="dish-side">
                <div class="dish-emoji">{{ item.emoji }}</div>
                @if (cart.qty(item.id) > 0) {
                  <div class="stepper">
                    <button type="button" aria-label="Remove one" (click)="cart.remove(item.id)">−</button>
                    <span>{{ cart.qty(item.id) }}</span>
                    <button type="button" aria-label="Add one" (click)="add(item)">+</button>
                  </div>
                } @else {
                  <button type="button" class="add-btn" (click)="add(item)">ADD</button>
                }
              </div>
            </div>
          }
        </section>
      }
    } @else {
      <p class="muted">Loading menu…</p>
    }

    @if (cart.count() > 0) {
      <a class="cartbar" routerLink="/cart">
        <span>{{ cart.count() }} item(s) · {{ cart.subtotal() | currency: 'INR' : 'symbol-narrow' : '1.0-0' }}</span>
        <span>View cart →</span>
      </a>
    }
  `
})
export class RestaurantDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly cart = inject(CartService);

  protected readonly restaurant = signal<RestaurantDetail | null>(null);
  protected readonly error = signal('');
  protected readonly vegOnly = signal(false);

  protected readonly categories = computed(() => {
    const r = this.restaurant();
    if (!r) return [];
    const groups = new Map<string, MenuItem[]>();
    for (const item of r.menu) {
      if (this.vegOnly() && !item.veg) continue;
      const list = groups.get(item.category) ?? [];
      list.push(item);
      groups.set(item.category, list);
    }
    return [...groups].map(([name, items]) => ({ name, items }));
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id)) {
      this.router.navigateByUrl('/restaurants');
      return;
    }
    this.api.restaurant(id).subscribe({
      next: r => this.restaurant.set(r),
      error: () => this.error.set('Restaurant not found.')
    });
  }

  protected add(item: MenuItem): void {
    const r = this.restaurant();
    if (r) this.cart.add(item, { id: r.id, name: r.name });
  }
}
