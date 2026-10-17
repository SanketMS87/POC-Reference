import { TestBed } from '@angular/core/testing';
import { MenuItem } from './models';
import { CartService } from './cart.service';

const item = (id: number, price: number, name = `Item ${id}`): MenuItem => ({
  id, name, category: 'Mains', price, veg: true, bestseller: false, emoji: '🍽️'
});
const pizza = { id: 1, name: 'Pizza Piazza' };
const burger = { id: 2, name: 'Burger Barn' };

describe('CartService', () => {
  let cart: CartService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    cart = TestBed.inject(CartService);
  });

  afterEach(() => jest.restoreAllMocks());

  it('starts empty with zero totals', () => {
    expect(cart.count()).toBe(0);
    expect(cart.subtotal()).toBe(0);
    expect(cart.deliveryFee()).toBe(0);
    expect(cart.platformFee()).toBe(0);
    expect(cart.total()).toBe(0);
    expect(cart.restaurant()).toBeNull();
  });

  it('charges delivery below the free-delivery threshold', () => {
    cart.add(item(1, 200), pizza);
    expect(cart.subtotal()).toBe(200);
    expect(cart.deliveryFee()).toBe(40);
    expect(cart.tax()).toBe(10);
    expect(cart.platformFee()).toBe(5);
    expect(cart.total()).toBe(255);
  });

  it('delivers free at and above the threshold of 399', () => {
    cart.add(item(1, 399), pizza);
    expect(cart.deliveryFee()).toBe(0);
    expect(cart.total()).toBe(399 + 19.95 + 5);
    expect(cart.freeDeliveryMin).toBe(399);
  });

  it('charges delivery just below the threshold', () => {
    cart.add(item(1, 398), pizza);
    expect(cart.deliveryFee()).toBe(40);
  });

  it('merges repeated adds into one line and counts quantities', () => {
    cart.add(item(1, 100), pizza);
    cart.add(item(1, 100), pizza);
    cart.add(item(2, 50), pizza);
    expect(cart.lines().length).toBe(2);
    expect(cart.qty(1)).toBe(2);
    expect(cart.qty(2)).toBe(1);
    expect(cart.qty(99)).toBe(0);
    expect(cart.count()).toBe(3);
    expect(cart.subtotal()).toBe(250);
  });

  it('rounds tax to two decimals', () => {
    cart.add(item(1, 10.1), pizza);
    expect(cart.tax()).toBe(0.51);
  });

  it('removes one unit at a time and clears the restaurant when empty', () => {
    cart.add(item(1, 100), pizza);
    cart.add(item(1, 100), pizza);
    cart.remove(1);
    expect(cart.qty(1)).toBe(1);
    expect(cart.restaurant()).toEqual(pizza);
    cart.remove(1);
    expect(cart.count()).toBe(0);
    expect(cart.restaurant()).toBeNull();
  });

  it('ignores removal of items that are not in the cart', () => {
    cart.add(item(1, 100), pizza);
    cart.remove(42);
    expect(cart.qty(1)).toBe(1);
  });

  it('asks before replacing a cart from another restaurant and keeps it when declined', () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    cart.add(item(1, 100), pizza);
    cart.add(item(2, 90), burger);
    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('Pizza Piazza'));
    expect(cart.restaurant()).toEqual(pizza);
    expect(cart.qty(2)).toBe(0);
  });

  it('replaces the cart when the user confirms', () => {
    jest.spyOn(window, 'confirm').mockReturnValue(true);
    cart.add(item(1, 100), pizza);
    cart.add(item(2, 90), burger);
    expect(cart.restaurant()).toEqual(burger);
    expect(cart.qty(1)).toBe(0);
    expect(cart.qty(2)).toBe(1);
  });

  it('does not prompt when adding from the same restaurant', () => {
    const confirm = jest.spyOn(window, 'confirm');
    cart.add(item(1, 100), pizza);
    cart.add(item(2, 90), pizza);
    expect(confirm).not.toHaveBeenCalled();
  });

  it('clear empties everything', () => {
    cart.add(item(1, 100), pizza);
    cart.clear();
    expect(cart.count()).toBe(0);
    expect(cart.restaurant()).toBeNull();
  });
});
