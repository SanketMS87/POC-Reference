import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Order, OrderRequest, RestaurantDetail, RestaurantFilters, RestaurantSummary } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  restaurants(filters: RestaurantFilters): Observable<RestaurantSummary[]> {
    let params = new HttpParams();
    if (filters.q?.trim()) params = params.set('q', filters.q.trim());
    if (filters.cuisine) params = params.set('cuisine', filters.cuisine);
    if (filters.vegOnly) params = params.set('vegOnly', true);
    if (filters.topRated) params = params.set('topRated', true);
    if (filters.fast) params = params.set('fast', true);
    if (filters.sort) params = params.set('sort', filters.sort);
    return this.http.get<RestaurantSummary[]>(`${this.base}/restaurants`, { params });
  }

  restaurant(id: number): Observable<RestaurantDetail> {
    return this.http.get<RestaurantDetail>(`${this.base}/restaurants/${id}`);
  }

  placeOrder(request: OrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders`, request);
  }

  orders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/orders`);
  }
}
