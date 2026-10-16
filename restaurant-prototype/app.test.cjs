const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, 'app.js'), 'utf8');

function boot(storage = new Map(), session = new Map()) {
  const elements = new Map();
  const events = new Map();
  const classes = () => {
    const values = new Set();
    return { add: x => values.add(x), remove: x => values.delete(x), contains: x => values.has(x) };
  };
  function element(selector) {
    if (!elements.has(selector)) elements.set(selector, {
      innerHTML: '', textContent: '', value: '', hidden: true, type: 'password',
      dataset: {}, classList: classes(), focus() { this.focused = true; },
      scrollIntoView() { this.scrolled = true; }, setAttribute(k, v) { this[k] = v; },
      addEventListener(k, fn) { events.set(selector + ':' + k, fn); }
    });
    return elements.get(selector);
  }
  const storageApi = map => ({ getItem: k => map.get(k) ?? null, setItem: (k, v) => map.set(k, v), removeItem: k => map.delete(k) });
  const sandbox = {
    document: { querySelector: element, addEventListener: (k, fn) => events.set(k, fn) },
    localStorage: storageApi(storage), sessionStorage: storageApi(session),
    window: { scrollTo() {} }, confirm: () => true,
    setTimeout: () => 1, clearTimeout() {}, setInterval: () => 1, clearInterval() {}, console
  };
  const context = vm.createContext(sandbox);
  vm.runInContext(source, context);
  return { run: code => vm.runInContext(code, context), element, events, storage, session, sandbox };
}

test('home renders all 26 complete, uniquely identified restaurant menus', () => {
  const app = boot();
  assert.match(app.element('#app').innerHTML, /26 restaurants near you/);
  assert.equal(app.run('RESTAURANTS.length'), 26);
  assert.equal(app.run('new Set(RESTAURANTS.map(r => r.id)).size'), 26);
  assert.equal(app.run('ITEMS.size'), app.run('RESTAURANTS.reduce((n, r) => n + r.menu.length, 0)'));
  assert.ok(app.run('RESTAURANTS.every(r => r.menu.length >= 5 && r.menu.every(d => d.id.startsWith(r.id + "-") && d.price > 0))'));
  assert.ok(app.run('RESTAURANTS.slice(18).every(r => CUISINES.some(([c]) => r.cuisines.includes(c)))'));
  for (const slot of ['breakfast', 'lunch', 'snacks', 'dinner', 'late']) {
    app.run('state.slot = "' + slot + '"; render()');
    assert.match(app.element('#app').innerHTML, /Big cravings/);
  }
});

test('every rendered action is wired, including login and search', () => {
  const app = boot();
  const home = app.element('#app').innerHTML;
  app.run('actions.login()');
  assert.match(app.element('#app').innerHTML, /loginForm/);
  const login = app.element('#app').innerHTML;
  app.run('actions.leaveLogin(); actions.openRest("r26"); actions.add("r26-1"); setQuery("pizza")');
  const html = home + login + app.element('#app').innerHTML + app.element('#drawer').innerHTML + fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8');
  for (const match of html.matchAll(/data-action="([^"]+)"/g)) {
    assert.equal(app.run('typeof actions[' + JSON.stringify(match[1]) + ']'), 'function', match[1]);
  }
});

test('explore, cuisine filters, filter reset, sorting and meal selection work', () => {
  const app = boot();
  app.run('actions.explore()');
  assert.ok(app.element('#restaurants').scrolled);
  app.run('actions.cuisine("Thai")');
  assert.match(app.element('#app').innerHTML, /1 restaurants near you/);
  app.run('actions.toggleFilter("veg"); actions.resetFilters()');
  assert.match(app.element('#app').innerHTML, /26 restaurants near you/);
  app.events.get('change')({ target: { dataset: { change: 'sort' }, value: 'eta' } });
  assert.equal(app.run('sortRests(RESTAURANTS)[0].eta'), app.run('Math.min(...RESTAURANTS.map(r => r.eta))'));
  app.events.get('change')({ target: { dataset: { change: 'slot' }, value: 'breakfast' } });
  assert.match(app.element('#app').innerHTML, /Good morning/);
});

test('new restaurants are searchable, browsable and support veg menus', () => {
  const app = boot();
  for (let n = 19; n <= 26; n++) {
    app.run('setQuery(REST_BY_ID["r' + n + '"].name)');
    assert.ok(app.element('#app').innerHTML.includes(app.run('esc(REST_BY_ID["r' + n + '"].name)')));
    app.run('actions.openRest("r' + n + '"); state.menuVeg = true; render()');
    assert.doesNotMatch(app.element('#app').innerHTML, /vdot nonveg/);
  }
  app.run('setQuery("veg pizza under 300")');
  assert.ok(app.run('runSearch(parseQuery(state.query)).every(x => x.dishes.every(d => d.veg && d.price <= 300))'));
  assert.ok(app.run('runSearch(parseQuery("pizzza")).length > 0'));
});

test('cart quantities, coupons, free-delivery threshold and persistence work', () => {
  const app = boot();
  app.run('actions.offer("WELCOME50"); actions.add("r2-1")');
  assert.equal(app.run('bill().discount'), 100);
  assert.equal(app.run('bill().delivery'), 40);
  app.run('actions.inc("r2-1"); actions.inc("r2-1")');
  assert.equal(app.run('cartCount()'), 3);
  assert.equal(app.run('bill().delivery'), 0);
  app.run('actions.dec("r2-1"); actions.removeCoupon()');
  assert.equal(app.run('bill().discount'), 0);
  const reloaded = boot(app.storage);
  assert.equal(reloaded.run('cartCount()'), 2);
  app.element('#couponIn').value = 'bad-code';
  app.run('actions.applyTyped()');
  assert.equal(app.element('#toast').textContent, 'Invalid coupon code');
  app.element('#couponIn').value = 'SAVE20';
  app.run('actions.applyTyped()');
  assert.equal(app.run('bill().discount'), 80);
});

test('cross-restaurant cart replacement respects confirmation', () => {
  const app = boot();
  app.run('actions.add("r1-1")');
  app.sandbox.confirm = () => false;
  app.run('actions.add("r19-1")');
  assert.equal(app.run('state.cart.restId'), 'r1');
  app.sandbox.confirm = () => true;
  app.run('actions.add("r19-1")');
  assert.equal(app.run('state.cart.restId'), 'r19');
  assert.equal(app.run('cartCount()'), 1);
});

test('demo login validates, reveals password, and returns checkout to the cart', () => {
  const app = boot();
  app.run('actions.add("r19-1"); actions.openCart(); actions.place()');
  assert.equal(app.run('state.view'), 'login');
  const submit = () => app.events.get('submit')({ target: { id: 'loginForm' }, preventDefault() {} });
  submit();
  assert.equal(app.element('#loginError').hidden, false);
  assert.equal(app.run('demoUser'), false);
  app.run('actions.fillDemo(); actions.togglePassword()');
  assert.equal(app.element('#loginPassword').type, 'text');
  app.run('actions.togglePassword()');
  assert.equal(app.element('#loginPassword').type, 'password');
  submit();
  assert.equal(app.run('demoUser'), true);
  assert.ok(app.element('#drawer').classList.contains('open'));
  assert.equal(app.run('state.orders.length'), 0);
  assert.equal(app.element('#accountBtn').textContent, 'Log out');
  assert.equal(boot(app.storage, app.session).run('demoUser'), true);
  assert.ok(!JSON.stringify([...app.session]).includes('Demo@123'));
  app.run('actions.place()');
  assert.equal(app.run('state.orders.length'), 1);
  assert.equal(app.run('cartCount()'), 0);
  assert.equal(app.element('#modal').hidden, false);
  app.run('state.orders[0].placedAt = Date.now() - 18001; renderTracking()');
  assert.match(app.element('#modal').innerHTML, /Delivered/);
  app.run('actions.closeModal(); actions.reorder(state.orders[0].id)');
  assert.equal(app.run('cartCount()'), 1);
  app.run('actions.logout()');
  assert.equal(app.run('demoUser'), false);
});
