export type Role = 'CUSTOMER' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface MenuItem {
  id: number;
  name: string;
  category: string;
  price: number;
  veg: boolean;
  bestseller: boolean;
  emoji: string;
}

export interface RestaurantSummary {
  id: number;
  name: string;
  cuisines: string[];
  rating: number;
  etaMinutes: number;
  costForTwo: number;
  pureVeg: boolean;
  emoji: string;
  description: string;
}

export interface RestaurantDetail extends RestaurantSummary {
  menu: MenuItem[];
}

export interface RestaurantFilters {
  q?: string;
  cuisine?: string | null;
  vegOnly?: boolean;
  topRated?: boolean;
  fast?: boolean;
  sort?: string;
}

export interface OrderLine {
  name: string;
  price: number;
  quantity: number;
}

export interface Order {
  id: number;
  restaurantName: string;
  status: string;
  items: OrderLine[];
  subtotal: number;
  deliveryFee: number;
  tax: number;
  platformFee: number;
  total: number;
  createdAt: string;
}

export interface OrderRequest {
  items: { menuItemId: number; quantity: number }[];
}
