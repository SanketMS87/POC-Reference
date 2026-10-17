import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';

@Component({
  selector: 'app-cart',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <h1>Your cart</h1>

    @if (cart.count() === 0) {
      <div class="empty">
        <div class="big">🛒</div>
        <p>Your cart is empty.</p>
        <a class="primary inline" routerLink="/restaurants">Browse restaurants</a>
      </div>
    } @else {
      <div class="cart-wrap">
        <section class="panel">
          <h2>{{ cart.restaurant()?.name }}</h2>
          @for (l of cart.lines(); track l.item.id) {
            <div class="line">
              <span>
                <span class="vdot" [class.veg]="l.item.veg" [class.nonveg]="!l.item.veg"></span>
                {{ l.item.name }}
              </span>
              <span class="stepper">
                <button type="button" aria-label="Remove one" (click)="cart.remove(l.item.id)">−</button>
                <span>{{ l.qty }}</span>
                <button type="button" aria-label="Add one" (click)="cart.add(l.item, cart.restaurant()!)">+</button>
              </span>
              <span>{{ l.item.price * l.qty | currency: 'INR' : 'symbol-narrow' : '1.0-0' }}</span>
            </div>
          }
          <button type="button" class="link" (click)="cart.clear()">Clear cart</button>
        </section>

        <section class="panel">
          @if (cart.deliveryFee() > 0) {
            <div class="nudge">
              Add <b>{{ cart.freeDeliveryMin - cart.subtotal() | currency: 'INR' : 'symbol-narrow' : '1.0-0' }}</b> more for free delivery
              <div class="bar"><i [style.width.%]="(cart.subtotal() / cart.freeDeliveryMin) * 100"></i></div>
            </div>
          } @else {
            <div class="nudge">🎉 You unlocked free delivery</div>
          }

          <h2>Bill details</h2>
          <div class="bill">
            <div class="line"><span>Item total</span><span>{{ cart.subtotal() | currency: 'INR' : 'symbol-narrow' : '1.2-2' }}</span></div>
            <div class="line"><span>Delivery fee</span><span>{{ cart.deliveryFee() ? (cart.deliveryFee() | currency: 'INR' : 'symbol-narrow' : '1.2-2') : 'FREE' }}</span></div>
            <div class="line"><span>GST (5%)</span><span>{{ cart.tax() | currency: 'INR' : 'symbol-narrow' : '1.2-2' }}</span></div>
            <div class="line"><span>Platform fee</span><span>{{ cart.platformFee() | currency: 'INR' : 'symbol-narrow' : '1.2-2' }}</span></div>
            <div class="line total"><span>To pay</span><span>{{ cart.total() | currency: 'INR' : 'symbol-narrow' : '1.2-2' }}</span></div>
          </div>

          @if (error()) { <div class="alert">{{ error() }}</div> }

          <button type="button" class="primary" [disabled]="placing()" (click)="place()">
            {{ placing() ? 'Placing order…' : 'Place order' }}
          </button>
        </section>
      </div>
    }
  `
})
export class CartPage {
  protected readonly cart = inject(CartService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly placing = signal(false);
  protected readonly error = signal('');

  protected place(): void {
    this.placing.set(true);
    this.error.set('');
    const items = this.cart.lines().map(l => ({ menuItemId: l.item.id, quantity: l.qty }));
    this.api.placeOrder({ items }).subscribe({
      next: order => {
        this.cart.clear();
        this.router.navigate(['/orders'], { queryParams: { placed: order.id } });
      },
      error: (e: HttpErrorResponse) => {
        this.placing.set(false);
        this.error.set(e.error?.message ?? 'Could not place the order.');
      }
    });
  }
}
