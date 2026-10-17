import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Order } from '../core/models';

const STATUS_LABELS: Record<string, string> = {
  PLACED: 'Order placed',
  PREPARING: 'Preparing',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered'
};

@Component({
  selector: 'app-orders',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <h1>My orders</h1>

    @if (placedId()) {
      <div class="alert success">🎉 Order #{{ placedId() }} placed successfully!</div>
    }
    @if (error()) { <div class="alert">{{ error() }}</div> }

    @if (loading()) {
      <p class="muted">Loading orders…</p>
    } @else if (orders().length === 0 && !error()) {
      <div class="empty">
        <p>No orders yet.</p>
        <a class="primary inline" routerLink="/restaurants">Order something tasty</a>
      </div>
    } @else {
      @for (o of orders(); track o.id) {
        <article class="panel order">
          <div class="row">
            <h3>{{ o.restaurantName }}</h3>
            <span class="tag green">{{ label(o.status) }}</span>
          </div>
          <p class="muted">#{{ o.id }} · {{ o.createdAt | date: 'medium' }}</p>
          <ul class="items">
            @for (i of o.items; track $index) {
              <li>{{ i.quantity }} × {{ i.name }}</li>
            }
          </ul>
          <div class="row">
            <span class="muted">Delivery {{ o.deliveryFee | currency: 'INR' : 'symbol-narrow' : '1.0-0' }} · GST {{ o.tax | currency: 'INR' : 'symbol-narrow' : '1.2-2' }}</span>
            <b>{{ o.total | currency: 'INR' : 'symbol-narrow' : '1.2-2' }}</b>
          </div>
        </article>
      }
    }
  `
})
export class OrdersPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly orders = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly placedId = signal<string | null>(null);

  ngOnInit(): void {
    this.placedId.set(this.route.snapshot.queryParamMap.get('placed'));
    this.api.orders().subscribe({
      next: list => {
        this.orders.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your orders.');
        this.loading.set(false);
      }
    });
  }

  protected label(status: string): string {
    return STATUS_LABELS[status] ?? status;
  }
}
