import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login').then(m => m.LoginPage)
  },
  {
    path: 'restaurants',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard').then(m => m.DashboardPage)
  },
  {
    path: 'restaurants/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/restaurant-detail').then(m => m.RestaurantDetailPage)
  },
  {
    path: 'cart',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/cart').then(m => m.CartPage)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/orders').then(m => m.OrdersPage)
  },
  { path: '', pathMatch: 'full', redirectTo: 'restaurants' },
  { path: '**', redirectTo: 'restaurants' }
];
