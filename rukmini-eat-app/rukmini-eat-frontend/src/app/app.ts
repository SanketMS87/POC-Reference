import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { CartService } from './core/cart.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.isLoggedIn()) {
      <header class="topbar">
        <a class="brand" routerLink="/restaurants">
          <img class="brand-icon" src="logo-icon.svg" alt="">
          <span class="brand-text">Rukmini<small>EAT APP</small></span>
        </a>
        <nav class="nav">
          <a routerLink="/restaurants" routerLinkActive="active">Restaurants</a>
          <a routerLink="/orders" routerLinkActive="active">My orders</a>
        </nav>
        <a class="cart-btn" routerLink="/cart">
          🛒 Cart
          @if (cart.count() > 0) {
            <span class="count">{{ cart.count() }}</span>
          }
        </a>
        <div class="user">
          <span>👤 {{ auth.user()?.name }}</span>
          <button type="button" class="link" (click)="auth.logout()">Logout</button>
        </div>
      </header>
      <nav class="tabbar" aria-label="Main">
        <a routerLink="/restaurants" routerLinkActive="active"><span class="ti">🍽️</span>Home</a>
        <a routerLink="/orders" routerLinkActive="active"><span class="ti">🧾</span>Orders</a>
        <a routerLink="/cart" routerLinkActive="active">
          <span class="ti">🛒 @if (cart.count() > 0) { <b class="tb">{{ cart.count() }}</b> }</span>Cart
        </a>
        <button type="button" (click)="auth.logout()"><span class="ti">🚪</span>Logout</button>
      </nav>
    }
    <main [class.page]="auth.isLoggedIn()">
      <router-outlet />
    </main>
  `
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
}
