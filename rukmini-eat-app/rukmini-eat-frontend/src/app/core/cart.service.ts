import { Injectable, computed, signal } from '@angular/core';
import { MenuItem } from './models';

export interface CartLine {
  item: MenuItem;
  qty: number;
}

const FREE_DELIVERY_MIN = 399;
const DELIVERY_FEE = 40;
const PLATFORM_FEE = 5;
const GST_RATE = 0.05;
const round2 = (n: number) => Math.round(n * 100) / 100;

/** Client-side estimate only; the server recalculates prices when the order is placed. */
@Injectable({ providedIn: 'root' })
export class CartService {
  readonly freeDeliveryMin = FREE_DELIVERY_MIN;
  readonly restaurant = signal<{ id: number; name: string } | null>(null);
  readonly lines = signal<CartLine[]>([]);

  readonly count = computed(() => this.lines().reduce((n, l) => n + l.qty, 0));
  readonly subtotal = computed(() => this.lines().reduce((s, l) => s + l.item.price * l.qty, 0));
  readonly deliveryFee = computed(() => (this.subtotal() === 0 || this.subtotal() >= FREE_DELIVERY_MIN ? 0 : DELIVERY_FEE));
  readonly tax = computed(() => round2(this.subtotal() * GST_RATE));
  readonly platformFee = computed(() => (this.subtotal() === 0 ? 0 : PLATFORM_FEE));
  readonly total = computed(() => round2(this.subtotal() + this.deliveryFee() + this.tax() + this.platformFee()));

  qty(itemId: number): number {
    return this.lines().find(l => l.item.id === itemId)?.qty ?? 0;
  }

  add(item: MenuItem, restaurant: { id: number; name: string }): void {
    const current = this.restaurant();
    if (current && current.id !== restaurant.id && this.count() > 0) {
      if (!confirm(`Your cart has items from ${current.name}. Replace them with items from ${restaurant.name}?`)) {
        return;
      }
      this.lines.set([]);
    }
    this.restaurant.set(restaurant);
    this.lines.update(lines => {
      const existing = lines.find(l => l.item.id === item.id);
      return existing
        ? lines.map(l => (l.item.id === item.id ? { ...l, qty: l.qty + 1 } : l))
        : [...lines, { item, qty: 1 }];
    });
  }

  remove(itemId: number): void {
    this.lines.update(lines =>
      lines.map(l => (l.item.id === itemId ? { ...l, qty: l.qty - 1 } : l)).filter(l => l.qty > 0)
    );
    if (this.lines().length === 0) this.restaurant.set(null);
  }

  clear(): void {
    this.lines.set([]);
    this.restaurant.set(null);
  }
}
