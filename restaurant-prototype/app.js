'use strict';

/* ---------- helpers ---------- */
const $ = (s, el = document) => el.querySelector(s);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '₹' + Math.round(n);
const norm = s => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
const words = s => norm(s).split(/[^a-z0-9]+/).filter(Boolean);

function lev(a, b) {
  if (a === b) return 0;
  if (Math.abs(a.length - b.length) > 2) return 3;
  let prev = Array.from({ length: b.length + 1 }, (_, j) => j);
  for (let i = 1; i <= a.length; i++) {
    const cur = [i];
    for (let j = 1; j <= b.length; j++) {
      cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    }
    prev = cur;
  }
  return prev[b.length];
}

const load = (k, fb) => { try { return JSON.parse(localStorage.getItem(k)) ?? fb; } catch { return fb; } };
const save = (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* storage unavailable */ } };

/* ---------- mock data ---------- */
const m = (id, name, price, veg, emoji, cat, best = false) => ({ id, name, price, veg, emoji, cat, best });

const RESTAURANTS = [
  { id: 'r1', name: 'Spice Route Biryani', cuisines: ['Biryani', 'North Indian', 'Mughlai'], rating: 4.5, votes: '12K+', eta: 32, costForTwo: 600, pureVeg: false, emoji: '🍛', offers: true, menu: [
    m('r1-1', 'Hyderabadi Chicken Dum Biryani', 289, false, '🍗', 'Biryani', true),
    m('r1-2', 'Mutton Biryani', 389, false, '🍖', 'Biryani'),
    m('r1-3', 'Paneer Tikka Biryani', 249, true, '🧀', 'Biryani'),
    m('r1-4', 'Veg Dum Biryani', 219, true, '🥕', 'Biryani'),
    m('r1-5', 'Chicken 65', 199, false, '🍗', 'Starters'),
    m('r1-6', 'Paneer 65', 189, true, '🧀', 'Starters'),
    m('r1-7', 'Mirchi Ka Salan', 79, true, '🌶️', 'Sides'),
    m('r1-8', 'Boondi Raita', 49, true, '🥛', 'Sides'),
    m('r1-9', 'Masala Soda', 59, true, '🥤', 'Drinks'),
    m('r1-10', 'Double Ka Meetha', 99, true, '🍮', 'Desserts') ] },
  { id: 'r2', name: 'Pizza Piazza', cuisines: ['Pizza', 'Italian', 'Fast Food'], rating: 4.3, votes: '8K+', eta: 25, costForTwo: 450, pureVeg: false, emoji: '🍕', offers: true, menu: [
    m('r2-1', 'Margherita Pizza', 199, true, '🍕', 'Pizzas', true),
    m('r2-2', 'Farmhouse Pizza', 299, true, '🍕', 'Pizzas'),
    m('r2-3', 'Pepperoni Pizza', 349, false, '🍕', 'Pizzas'),
    m('r2-4', 'BBQ Chicken Pizza', 359, false, '🍕', 'Pizzas'),
    m('r2-5', 'Garlic Bread', 119, true, '🥖', 'Sides'),
    m('r2-6', 'Peri Peri Fries', 129, true, '🍟', 'Sides'),
    m('r2-7', 'Cola', 60, true, '🥤', 'Drinks'),
    m('r2-8', 'Iced Tea', 89, true, '🧋', 'Drinks'),
    m('r2-9', 'Choco Lava Cake', 99, true, '🍫', 'Desserts') ] },
  { id: 'r3', name: 'Burger Barn', cuisines: ['Burgers', 'American', 'Fast Food'], rating: 4.2, votes: '6K+', eta: 22, costForTwo: 350, pureVeg: false, emoji: '🍔', offers: false, menu: [
    m('r3-1', 'Classic Veg Burger', 129, true, '🍔', 'Burgers'),
    m('r3-2', 'Crispy Chicken Burger', 179, false, '🍔', 'Burgers', true),
    m('r3-3', 'Double Patty Beast', 249, false, '🍔', 'Burgers'),
    m('r3-4', 'Aloo Tikki Supreme', 149, true, '🍔', 'Burgers'),
    m('r3-5', 'Fries', 99, true, '🍟', 'Sides'),
    m('r3-6', 'Onion Rings', 109, true, '🧅', 'Sides'),
    m('r3-7', 'Chicken Nuggets', 139, false, '🍗', 'Sides'),
    m('r3-8', 'Cold Coffee', 119, true, '🥤', 'Drinks'),
    m('r3-9', 'Lemonade', 79, true, '🍋', 'Drinks'),
    m('r3-10', 'Brownie Sundae', 149, true, '🍨', 'Desserts') ] },
  { id: 'r4', name: 'Dosa Darbar', cuisines: ['South Indian', 'Breakfast'], rating: 4.6, votes: '15K+', eta: 20, costForTwo: 250, pureVeg: true, emoji: '🥞', offers: true, menu: [
    m('r4-1', 'Masala Dosa', 99, true, '🥞', 'Dosas', true),
    m('r4-2', 'Ghee Roast Dosa', 119, true, '🥞', 'Dosas'),
    m('r4-3', 'Mysore Masala Dosa', 129, true, '🥞', 'Dosas'),
    m('r4-4', 'Set Dosa', 89, true, '🥞', 'Dosas'),
    m('r4-5', 'Idli (3 pcs)', 69, true, '🍚', 'Tiffin'),
    m('r4-6', 'Medu Vada', 79, true, '🍩', 'Tiffin'),
    m('r4-7', 'Ven Pongal', 89, true, '🍚', 'Tiffin'),
    m('r4-8', 'Filter Coffee', 49, true, '☕', 'Drinks'),
    m('r4-9', 'Buttermilk', 39, true, '🥛', 'Drinks'),
    m('r4-10', 'Kesari Bath', 59, true, '🍮', 'Desserts') ] },
  { id: 'r5', name: 'Wok This Way', cuisines: ['Chinese', 'Asian'], rating: 4.1, votes: '5K+', eta: 30, costForTwo: 450, pureVeg: false, emoji: '🥡', offers: false, menu: [
    m('r5-1', 'Veg Hakka Noodles', 169, true, '🍜', 'Noodles & Rice'),
    m('r5-2', 'Chicken Schezwan Fried Rice', 199, false, '🍚', 'Noodles & Rice'),
    m('r5-3', 'Burnt Garlic Noodles', 179, true, '🍜', 'Noodles & Rice'),
    m('r5-4', 'Chilli Chicken', 229, false, '🍗', 'Starters', true),
    m('r5-5', 'Gobi Manchurian', 179, true, '🥦', 'Starters'),
    m('r5-6', 'Veg Spring Rolls', 149, true, '🌯', 'Starters'),
    m('r5-7', 'Hot & Sour Soup', 99, true, '🍲', 'Sides'),
    m('r5-8', 'Iced Lemon Tea', 79, true, '🧋', 'Drinks'),
    m('r5-9', 'Honey Noodles with Ice Cream', 129, true, '🍨', 'Desserts') ] },
  { id: 'r6', name: 'Green Bowl', cuisines: ['Healthy', 'Salads', 'Continental'], rating: 4.4, votes: '4K+', eta: 24, costForTwo: 500, pureVeg: true, emoji: '🥗', offers: true, menu: [
    m('r6-1', 'Quinoa Power Bowl', 249, true, '🥗', 'Bowls', true),
    m('r6-2', 'Paneer Tikka Protein Bowl', 279, true, '🥙', 'Bowls'),
    m('r6-3', 'Mediterranean Bowl', 269, true, '🥗', 'Bowls'),
    m('r6-4', 'Greek Salad', 199, true, '🥒', 'Salads'),
    m('r6-5', 'Sprout Chaat Salad', 159, true, '🌱', 'Salads'),
    m('r6-6', 'Hummus & Pita', 139, true, '🫓', 'Sides'),
    m('r6-7', 'Detox Green Juice', 129, true, '🥬', 'Drinks'),
    m('r6-8', 'Watermelon Cooler', 99, true, '🍉', 'Drinks'),
    m('r6-9', 'Chia Pudding', 149, true, '🍮', 'Desserts') ] },
  { id: 'r7', name: 'Roll Republic', cuisines: ['Rolls', 'Fast Food', 'Mughlai'], rating: 4.0, votes: '7K+', eta: 21, costForTwo: 300, pureVeg: false, emoji: '🌯', offers: true, menu: [
    m('r7-1', 'Chicken Kathi Roll', 139, false, '🌯', 'Rolls', true),
    m('r7-2', 'Egg Roll', 99, false, '🌯', 'Rolls'),
    m('r7-3', 'Paneer Tikka Roll', 129, true, '🌯', 'Rolls'),
    m('r7-4', 'Mutton Seekh Roll', 179, false, '🌯', 'Rolls'),
    m('r7-5', 'Masala Fries', 89, true, '🍟', 'Sides'),
    m('r7-6', 'Cold Drink', 50, true, '🥤', 'Drinks'),
    m('r7-7', 'Sweet Lassi', 79, true, '🥛', 'Drinks'),
    m('r7-8', 'Gulab Jamun (2 pcs)', 59, true, '🍡', 'Desserts') ] },
  { id: 'r8', name: 'Sweet Tooth Studio', cuisines: ['Desserts', 'Bakery'], rating: 4.7, votes: '9K+', eta: 27, costForTwo: 400, pureVeg: true, emoji: '🍰', offers: true, menu: [
    m('r8-1', 'Belgian Chocolate Truffle Slice', 129, true, '🍰', 'Desserts', true),
    m('r8-2', 'Red Velvet Jar', 149, true, '🧁', 'Desserts'),
    m('r8-3', 'Tiramisu Cup', 179, true, '🍮', 'Desserts'),
    m('r8-4', 'Death by Chocolate Sundae', 189, true, '🍨', 'Desserts'),
    m('r8-5', 'Mango Sorbet', 119, true, '🍧', 'Desserts'),
    m('r8-6', 'Choco Chip Cookie', 69, true, '🍪', 'Bakery'),
    m('r8-7', 'Walnut Brownie', 99, true, '🍫', 'Bakery'),
    m('r8-8', 'Hot Chocolate', 129, true, '☕', 'Drinks'),
    m('r8-9', 'Cold Brew Coffee', 139, true, '🥤', 'Drinks') ] },
  { id: 'r9', name: 'Tandoor Tales', cuisines: ['North Indian', 'Tandoor', 'Mughlai'], rating: 4.4, votes: '10K+', eta: 35, costForTwo: 700, pureVeg: false, emoji: '🥘', offers: true, menu: [
    m('r9-1', 'Butter Chicken', 329, false, '🍗', 'Mains', true),
    m('r9-2', 'Dal Makhani', 249, true, '🥘', 'Mains'),
    m('r9-3', 'Paneer Butter Masala', 289, true, '🧀', 'Mains'),
    m('r9-4', 'Kadai Chicken', 319, false, '🍗', 'Mains'),
    m('r9-5', 'Butter Naan', 59, true, '🫓', 'Breads'),
    m('r9-6', 'Garlic Naan', 69, true, '🫓', 'Breads'),
    m('r9-7', 'Tandoori Roti', 29, true, '🫓', 'Breads'),
    m('r9-8', 'Tandoori Chicken (Half)', 299, false, '🍗', 'Starters'),
    m('r9-9', 'Hara Bhara Kebab', 219, true, '🥬', 'Starters'),
    m('r9-10', 'Sweet Lassi', 89, true, '🥛', 'Drinks'),
    m('r9-11', 'Phirni', 89, true, '🍮', 'Desserts') ] },
  { id: 'r10', name: 'Brew & Bites Café', cuisines: ['Café', 'Beverages', 'Snacks'], rating: 4.3, votes: '3K+', eta: 18, costForTwo: 350, pureVeg: false, emoji: '☕', offers: false, menu: [
    m('r10-1', 'Veg Club Sandwich', 169, true, '🥪', 'Snacks', true),
    m('r10-2', 'Chicken Panini', 199, false, '🥪', 'Snacks'),
    m('r10-3', 'Cheese Maggi', 99, true, '🍜', 'Snacks'),
    m('r10-4', 'Peri Peri Fries', 119, true, '🍟', 'Sides'),
    m('r10-5', 'Cappuccino', 129, true, '☕', 'Drinks'),
    m('r10-6', 'Cold Coffee', 139, true, '🥤', 'Drinks'),
    m('r10-7', 'Iced Americano', 119, true, '🧊', 'Drinks'),
    m('r10-8', 'Masala Chai', 59, true, '🍵', 'Drinks'),
    m('r10-9', 'Blueberry Muffin', 89, true, '🧁', 'Desserts') ] },
  { id: 'r11', name: 'Momo Mountain', cuisines: ['Momos', 'Asian', 'Chinese'], rating: 4.6, votes: '2K+', eta: 23, costForTwo: 300, pureVeg: false, emoji: '🥟', offers: true, menu: [
    m('r11-1', 'Steamed Veg Momos', 129, true, '🥟', 'Momos', true),
    m('r11-2', 'Chicken Chilli Momos', 179, false, '🥟', 'Momos'),
    m('r11-3', 'Paneer Tandoori Momos', 169, true, '🥟', 'Momos'),
    m('r11-4', 'Crispy Corn', 119, true, '🌽', 'Sides'),
    m('r11-5', 'Peach Iced Tea', 79, true, '🧋', 'Drinks') ] },
  { id: 'r12', name: 'Punjab on a Plate', cuisines: ['North Indian', 'Punjabi'], rating: 4.5, votes: '6K+', eta: 33, costForTwo: 550, pureVeg: true, emoji: '🥘', offers: true, menu: [
    m('r12-1', 'Punjabi Thali', 249, true, '🥘', 'Thalis', true),
    m('r12-2', 'Chole Bhature', 159, true, '🫓', 'Mains'),
    m('r12-3', 'Rajma Chawal', 179, true, '🍚', 'Mains'),
    m('r12-4', 'Butter Roti', 35, true, '🫓', 'Breads'),
    m('r12-5', 'Mango Lassi', 89, true, '🥭', 'Drinks') ] },
  { id: 'r13', name: 'Sushi & Seoul', cuisines: ['Japanese', 'Korean', 'Asian'], rating: 4.7, votes: '1K+', eta: 38, costForTwo: 900, pureVeg: false, emoji: '🍣', offers: false, menu: [
    m('r13-1', 'Avocado Sushi Roll', 299, true, '🍣', 'Sushi', true),
    m('r13-2', 'Salmon Sushi Roll', 399, false, '🍣', 'Sushi'),
    m('r13-3', 'Korean Chicken Bowl', 329, false, '🍚', 'Bowls'),
    m('r13-4', 'Edamame', 149, true, '🫛', 'Sides'),
    m('r13-5', 'Yuzu Lemonade', 119, true, '🍋', 'Drinks') ] },
  { id: 'r14', name: 'Taco Loco', cuisines: ['Mexican', 'Fast Food'], rating: 4.4, votes: '3K+', eta: 24, costForTwo: 450, pureVeg: false, emoji: '🌮', offers: true, menu: [
    m('r14-1', 'Roasted Veg Tacos', 189, true, '🌮', 'Tacos', true),
    m('r14-2', 'Chipotle Chicken Tacos', 229, false, '🌮', 'Tacos'),
    m('r14-3', 'Bean & Cheese Burrito', 219, true, '🌯', 'Burritos'),
    m('r14-4', 'Loaded Nachos', 159, true, '🧀', 'Sides'),
    m('r14-5', 'Churros with Chocolate', 129, true, '🍫', 'Desserts') ] },
  { id: 'r15', name: 'Coastal Curry House', cuisines: ['Seafood', 'South Indian', 'Kerala'], rating: 4.5, votes: '5K+', eta: 34, costForTwo: 700, pureVeg: false, emoji: '🍤', offers: true, menu: [
    m('r15-1', 'Malabar Fish Curry', 319, false, '🐟', 'Mains', true),
    m('r15-2', 'Prawn Ghee Roast', 359, false, '🍤', 'Mains'),
    m('r15-3', 'Vegetable Stew', 189, true, '🥘', 'Mains'),
    m('r15-4', 'Appam (3 pcs)', 89, true, '🥞', 'Breads'),
    m('r15-5', 'Tender Coconut Cooler', 79, true, '🥥', 'Drinks') ] },
  { id: 'r16', name: 'The Waffle Window', cuisines: ['Waffles', 'Desserts', 'Bakery'], rating: 4.6, votes: '4K+', eta: 19, costForTwo: 350, pureVeg: true, emoji: '🧇', offers: true, menu: [
    m('r16-1', 'Belgian Chocolate Waffle', 179, true, '🧇', 'Waffles', true),
    m('r16-2', 'Strawberry Cream Waffle', 199, true, '🍓', 'Waffles'),
    m('r16-3', 'Maple Butter Waffle', 149, true, '🧇', 'Waffles'),
    m('r16-4', 'Vanilla Ice Cream', 69, true, '🍨', 'Desserts'),
    m('r16-5', 'Chocolate Milkshake', 139, true, '🥤', 'Drinks') ] },
  { id: 'r17', name: 'Shawarma Street', cuisines: ['Arabian', 'Rolls', 'Lebanese'], rating: 4.3, votes: '7K+', eta: 21, costForTwo: 350, pureVeg: false, emoji: '🥙', offers: false, menu: [
    m('r17-1', 'Classic Chicken Shawarma', 149, false, '🌯', 'Rolls', true),
    m('r17-2', 'Falafel Wrap', 139, true, '🥙', 'Rolls'),
    m('r17-3', 'Shawarma Rice Bowl', 229, false, '🍚', 'Bowls'),
    m('r17-4', 'Hummus with Pita', 119, true, '🫓', 'Sides'),
    m('r17-5', 'Mint Lemon Cooler', 79, true, '🍋', 'Drinks') ] },
  { id: 'r18', name: 'Millet & Mint', cuisines: ['Healthy', 'Salads', 'South Indian'], rating: 4.8, votes: '1K+', eta: 25, costForTwo: 450, pureVeg: true, emoji: '🥑', offers: true, menu: [
    m('r18-1', 'Millet Buddha Bowl', 229, true, '🥗', 'Bowls', true),
    m('r18-2', 'Ragi Dosa', 129, true, '🥞', 'Dosas'),
    m('r18-3', 'Avocado Chickpea Salad', 249, true, '🥑', 'Salads'),
    m('r18-4', 'Baked Sweet Potato', 99, true, '🍠', 'Sides'),
    m('r18-5', 'Fresh Lime & Mint', 89, true, '🍋', 'Drinks') ] },
  { id: 'r19', name: 'Siam Basil Kitchen', cuisines: ['Thai', 'Asian'], rating: 4.6, votes: '2K+', eta: 36, costForTwo: 850, pureVeg: false, emoji: '🍜', offers: true, menu: [
    m('r19-1', 'Chicken Pad Thai', 329, false, '🍜', 'Noodles', true),
    m('r19-2', 'Thai Green Chicken Curry', 359, false, '🥘', 'Curries'),
    m('r19-3', 'Tofu Cashew Stir Fry', 279, true, '🥦', 'Mains'),
    m('r19-4', 'Tom Yum Prawn Soup', 249, false, '🍤', 'Soups'),
    m('r19-5', 'Jasmine Rice', 119, true, '🍚', 'Sides'),
    m('r19-6', 'Mango Sticky Rice', 199, true, '🥭', 'Desserts') ] },
  { id: 'r20', name: 'Kathiyawad Rasoi', cuisines: ['Gujarati', 'Thali'], rating: 4.7, votes: '4K+', eta: 28, costForTwo: 450, pureVeg: true, emoji: '🥘', offers: false, menu: [
    m('r20-1', 'Gujarati Thali', 249, true, '🥘', 'Thalis', true),
    m('r20-2', 'Undhiyu', 189, true, '🥦', 'Mains'),
    m('r20-3', 'Sev Tameta Nu Shaak', 149, true, '🍅', 'Mains'),
    m('r20-4', 'Khaman Dhokla (6 pcs)', 99, true, '🟨', 'Snacks'),
    m('r20-5', 'Methi Thepla (4 pcs)', 89, true, '🫓', 'Breads'),
    m('r20-6', 'Shrikhand', 99, true, '🍮', 'Desserts') ] },
  { id: 'r21', name: 'Sahyadri Spice House', cuisines: ['Maharashtrian', 'Regional Indian'], rating: 4.4, votes: '3K+', eta: 31, costForTwo: 550, pureVeg: false, emoji: '🌶️', offers: true, menu: [
    m('r21-1', 'Kolhapuri Chicken Rassa', 289, false, '🍗', 'Mains', true),
    m('r21-2', 'Misal Pav', 129, true, '🥘', 'Snacks'),
    m('r21-3', 'Pithla Bhakri', 159, true, '🫓', 'Mains'),
    m('r21-4', 'Sabudana Khichdi', 119, true, '🥣', 'Snacks'),
    m('r21-5', 'Puran Poli (2 pcs)', 109, true, '🫓', 'Desserts'),
    m('r21-6', 'Sol Kadhi', 69, true, '🥛', 'Drinks') ] },
  { id: 'r22', name: 'Chaat Chowk', cuisines: ['Street Food', 'Chaat', 'Snacks'], rating: 4.2, votes: '8K+', eta: 16, costForTwo: 220, pureVeg: true, emoji: '🥔', offers: false, menu: [
    m('r22-1', 'Pani Puri (6 pcs)', 59, true, '🥔', 'Chaat', true),
    m('r22-2', 'Dahi Papdi Chaat', 89, true, '🥣', 'Chaat'),
    m('r22-3', 'Pav Bhaji', 119, true, '🥘', 'Mains'),
    m('r22-4', 'Vada Pav (2 pcs)', 69, true, '🍔', 'Snacks'),
    m('r22-5', 'Bhel Puri', 79, true, '🥣', 'Chaat'),
    m('r22-6', 'Aloo Tikki Chaat', 99, true, '🥔', 'Chaat') ] },
  { id: 'r23', name: 'Marwar Bhoj', cuisines: ['Rajasthani', 'Thali'], rating: 4.6, votes: '2K+', eta: 39, costForTwo: 600, pureVeg: true, emoji: '🫓', offers: true, menu: [
    m('r23-1', 'Dal Baati Churma', 269, true, '🥘', 'Mains', true),
    m('r23-2', 'Gatte Ki Sabzi', 199, true, '🥘', 'Curries'),
    m('r23-3', 'Ker Sangri', 229, true, '🌱', 'Mains'),
    m('r23-4', 'Bajra Roti (2 pcs)', 69, true, '🫓', 'Breads'),
    m('r23-5', 'Pyaaz Kachori (2 pcs)', 89, true, '🧅', 'Snacks'),
    m('r23-6', 'Ghevar', 149, true, '🍯', 'Desserts') ] },
  { id: 'r24', name: 'Scoop & Swirl Creamery', cuisines: ['Ice Cream', 'Desserts'], rating: 4.8, votes: '5K+', eta: 17, costForTwo: 300, pureVeg: true, emoji: '🍦', offers: true, menu: [
    m('r24-1', 'Alphonso Mango Ice Cream', 109, true, '🥭', 'Scoops', true),
    m('r24-2', 'Belgian Chocolate Ice Cream', 119, true, '🍫', 'Scoops'),
    m('r24-3', 'Tender Coconut Ice Cream', 99, true, '🥥', 'Scoops'),
    m('r24-4', 'Roasted Pistachio Kulfi', 89, true, '🍦', 'Kulfi'),
    m('r24-5', 'Banana Split Sundae', 219, true, '🍌', 'Sundaes'),
    m('r24-6', 'Strawberry Cheesecake Ice Cream', 129, true, '🍓', 'Scoops') ] },
  { id: 'r25', name: 'Gongura Junction', cuisines: ['Andhra', 'South Indian'], rating: 4.5, votes: '6K+', eta: 33, costForTwo: 650, pureVeg: false, emoji: '🌶️', offers: false, menu: [
    m('r25-1', 'Gongura Chicken', 299, false, '🍗', 'Mains', true),
    m('r25-2', 'Andhra Veg Meals', 219, true, '🍚', 'Thalis'),
    m('r25-3', 'Gutti Vankaya Curry', 189, true, '🍆', 'Curries'),
    m('r25-4', 'Pesarattu with Ginger Chutney', 129, true, '🥞', 'Dosas'),
    m('r25-5', 'Royyala Vepudu', 349, false, '🍤', 'Starters'),
    m('r25-6', 'Pulihora', 139, true, '🍚', 'Rice') ] },
  { id: 'r26', name: 'Trattoria Olive', cuisines: ['Italian', 'Pasta', 'Continental'], rating: 4.7, votes: '2K+', eta: 42, costForTwo: 1100, pureVeg: false, emoji: '🍝', offers: true, menu: [
    m('r26-1', 'Spaghetti Aglio e Olio', 299, true, '🍝', 'Pasta', true),
    m('r26-2', 'Chicken Alfredo Penne', 379, false, '🍝', 'Pasta'),
    m('r26-3', 'Wild Mushroom Risotto', 349, true, '🍄', 'Risotto'),
    m('r26-4', 'Tomato Basil Bruschetta', 189, true, '🥖', 'Starters'),
    m('r26-5', 'Spinach Ricotta Ravioli', 359, true, '🥟', 'Pasta'),
    m('r26-6', 'Eggless Tiramisu', 199, true, '🍮', 'Desserts') ] }
];
RESTAURANTS.push(...EXTRA_RESTAURANTS);
RESTAURANTS.forEach((r, i) => { r.hue = (i * 36 + 10) % 360; });

const REST_BY_ID = Object.fromEntries(RESTAURANTS.map(r => [r.id, r]));
const ITEMS = new Map();
RESTAURANTS.forEach(r => r.menu.forEach(it => ITEMS.set(it.id, { item: it, rest: r })));

const CUISINES = [['Biryani', '🍛'], ['Pizza', '🍕'], ['Burgers', '🍔'], ['North Indian', '🥘'], ['Chinese', '🥡'], ['South Indian', '🥞'], ['Rolls', '🌯'], ['Healthy', '🥗'], ['Desserts', '🍰'], ['Café', '☕'], ['Momos', '🥟'], ['Mexican', '🌮'], ['Japanese', '🍣'], ['Seafood', '🍤'], ['Waffles', '🧇'], ['Thai', '🍜'], ['Gujarati', '🥘'], ['Maharashtrian', '🌶️'], ['Street Food', '🥔'], ['Rajasthani', '🫓'], ['Ice Cream', '🍦'], ['Andhra', '🌶️'], ['Italian', '🍝'], ...EXTRA_CUISINES];
const EXAMPLES = ['veg pizza under 300', 'fast biryani', 'top rated desserts', 'non veg burger below 200'];

const SLOTS = {
  breakfast: { label: 'Breakfast', greet: 'Good morning', emoji: '🌅', cuisines: ['South Indian', 'Café', 'Healthy'] },
  lunch: { label: 'Lunch', greet: 'Good afternoon', emoji: '☀️', cuisines: ['Biryani', 'North Indian', 'Healthy'] },
  snacks: { label: 'Snack time', greet: 'Good evening', emoji: '🌆', cuisines: ['Burgers', 'Rolls', 'Café', 'Desserts'] },
  dinner: { label: 'Dinner', greet: 'Good evening', emoji: '🌙', cuisines: ['North Indian', 'Pizza', 'Chinese', 'Biryani'] },
  late: { label: 'Late night', greet: 'Hello night owl', emoji: '🦉', cuisines: ['Burgers', 'Pizza', 'Desserts', 'Rolls'] }
};

const COUPONS = [
  { code: 'WELCOME50', desc: '50% off up to ₹100', min: 199, calc: s => Math.min(100, Math.round(s * 0.5)) },
  { code: 'SAVE20', desc: '20% off up to ₹80', min: 149, calc: s => Math.min(80, Math.round(s * 0.2)) },
  { code: 'BIGFEAST', desc: 'Flat ₹150 off', min: 699, calc: () => 150 }
];

const FREE_DELIVERY_MIN = 399;
const DELIVERY_FEE = 40;
const PLATFORM_FEE = 5;
const GST_RATE = 0.05;
const FAST_MAX = 25;
const TOP_MIN = 4.3;
const ADDON_CATS = ['Sides', 'Breads', 'Drinks', 'Desserts'];
const RIDERS = ['Arjun', 'Meera', 'Imran', 'Kavya', 'Rohan'];
const STAGE_MS = 6000; // demo speed: 6s per stage
const STAGES = [
  { k: 'Order placed', i: '🧾', d: 'Restaurant has received your order' },
  { k: 'Preparing', i: '👨‍🍳', d: 'Your food is being prepared' },
  { k: 'Out for delivery', i: '🛵', d: 'Your rider is on the way' },
  { k: 'Delivered', i: '✅', d: 'Enjoy your meal!' }
];

/* ---------- state ---------- */
const state = {
  query: '',
  filters: { veg: false, rating: false, fast: false, offers: false },
  cuisine: null,
  sort: 'relevance',
  slot: 'auto',
  view: 'home',
  restId: null,
  menuVeg: false,
  cart: load('fl_cart', { restId: null, items: {} }),
  coupon: 'auto', // 'auto' | 'none' | coupon code
  orders: load('fl_orders', []),
  trackId: null
};
let demoUser = false;
try { demoUser = sessionStorage.getItem('fl_demo_user') === 'true'; }
catch { toast('Session storage is unavailable. Login will last until you reload.'); }
let loginReturnView = 'home';
let checkoutAfterLogin = false;
// drop stale cart items that no longer exist
Object.keys(state.cart.items).forEach(id => { if (!ITEMS.has(id)) delete state.cart.items[id]; });

/* ---------- smart: time slot, ranking ---------- */
function autoSlot(h = new Date().getHours()) {
  if (h >= 5 && h < 11) return 'breakfast';
  if (h >= 11 && h < 16) return 'lunch';
  if (h >= 16 && h < 19) return 'snacks';
  if (h >= 19 && h < 23) return 'dinner';
  return 'late';
}
const slotKey = () => (state.slot === 'auto' ? autoSlot() : state.slot);
const slotMatch = r => r.cuisines.some(c => SLOTS[slotKey()].cuisines.includes(c));
const orderedBefore = id => state.orders.some(o => o.restId === id);

function relevance(r) {
  return r.rating * 2 - r.eta / 15 + (r.offers ? 0.5 : 0) + (slotMatch(r) ? 2 : 0) + (orderedBefore(r.id) ? 1.5 : 0);
}

const sorters = {
  relevance: (a, b) => relevance(b) - relevance(a),
  rating: (a, b) => b.rating - a.rating,
  eta: (a, b) => a.eta - b.eta,
  costLow: (a, b) => a.costForTwo - b.costForTwo,
  costHigh: (a, b) => b.costForTwo - a.costForTwo
};
const sortRests = list => [...list].sort(sorters[state.sort]);

function passesFilters(r) {
  const f = state.filters;
  if (f.veg && !r.pureVeg) return false;
  if (f.rating && r.rating < TOP_MIN) return false;
  if (f.fast && r.eta > FAST_MAX) return false;
  if (f.offers && !r.offers) return false;
  if (state.cuisine && !r.cuisines.includes(state.cuisine)) return false;
  return true;
}

/* ---------- smart: natural-language, typo-tolerant search ---------- */
const STOP = new Set(['in', 'on', 'the', 'and', 'with', 'for', 'me', 'want', 'some', 'i', 'a', 'to', 'of', 'near', 'delivery', 'food', 'order']);
const SYN = { sweets: 'dessert', sweet: 'dessert', coffee: 'cafe', chai: 'cafe', noodles: 'chinese', dosa: 'south', idli: 'south', paneer: 'indian', salad: 'healthy', salads: 'healthy', wrap: 'rolls', wraps: 'rolls' };

function parseQuery(q) {
  let s = norm(q).trim();
  const p = { veg: false, nonveg: false, max: null, fast: false, top: false, terms: [] };
  const mx = s.match(/(?:under|below|less than|within|<)\s*₹?\s*(\d+)/);
  if (mx) { p.max = Number(mx[1]); s = s.replace(mx[0], ' '); }
  if (/\bnon[- ]?veg(etarian)?\b/.test(s)) { p.nonveg = true; s = s.replace(/\bnon[- ]?veg(etarian)?\b/, ' '); }
  if (/\bveg(etarian)?\b/.test(s)) { p.veg = true; s = s.replace(/\bveg(etarian)?\b/, ' '); }
  if (/\b(fast|quick|quickly)\b/.test(s)) { p.fast = true; s = s.replace(/\b(fast|quick|quickly)\b/, ' '); }
  if (/\b(best|top|rated|popular)\b/.test(s)) { p.top = true; s = s.replace(/\b(best|top|rated|popular)\b/g, ' '); }
  p.terms = s.split(/[^a-z0-9]+/).filter(t => t.length > 1 && !STOP.has(t));
  return p;
}

function termMatches(t, ws) {
  const variants = SYN[t] ? [t, SYN[t]] : [t];
  return variants.some(v => ws.some(w =>
    w === v ||
    (v.length >= 3 && w.startsWith(v)) ||
    (w.length >= 4 && v.startsWith(w)) ||
    (v.length >= 4 && w.length >= 4 && lev(v, w) <= (v.length > 6 ? 2 : 1))
  ));
}

function runSearch(p) {
  const out = [];
  for (const r of RESTAURANTS) {
    if (p.fast && r.eta > FAST_MAX) continue;
    if (p.top && r.rating < TOP_MIN) continue;
    const dishFilters = p.veg || p.nonveg || p.max !== null;
    let menu = r.menu.filter(d => !(p.veg && !d.veg) && !(p.nonveg && d.veg) && !(p.max !== null && d.price > p.max));
    if (dishFilters && !menu.length) continue;
    const rWords = words(r.name + ' ' + r.cuisines.join(' '));
    const nameHit = p.terms.length > 0 && p.terms.every(t => termMatches(t, rWords));
    const dishes = p.terms.length ? menu.filter(d => { const dw = words(d.name + ' ' + d.cat); return p.terms.every(t => termMatches(t, dw)); }) : menu;
    if (p.terms.length && !dishes.length && !nameHit) continue;
    out.push({ r, dishes, score: (nameHit ? 5 : 0) + dishes.length + r.rating });
  }
  return out;
}

const VOCAB = [
  ...RESTAURANTS.map(r => ({ label: r.name, icon: '🏪', q: r.name })),
  ...CUISINES.map(([c, e]) => ({ label: c, icon: e, q: c })),
  ...[...ITEMS.values()].map(({ item }) => ({ label: item.name, icon: item.emoji, q: item.name }))
];
function suggest(q) {
  const t = words(q);
  if (norm(q).trim().length < 2 || !t.length) return [];
  const seen = new Set();
  return VOCAB.filter(v => {
    if (seen.has(v.label)) return false;
    const ok = norm(v.label).includes(norm(q).trim()) || t.every(x => termMatches(x, words(v.label)));
    if (ok) seen.add(v.label);
    return ok;
  }).slice(0, 6);
}

/* ---------- cart & billing ---------- */
const cartItems = () => Object.entries(state.cart.items)
  .map(([id, qty]) => ({ item: ITEMS.get(id)?.item, qty }))
  .filter(x => x.item);
const cartCount = () => cartItems().reduce((n, x) => n + x.qty, 0);
const persistCart = () => save('fl_cart', state.cart);

function addItem(id) {
  const { rest } = ITEMS.get(id);
  if (state.cart.restId && state.cart.restId !== rest.id && cartCount() > 0) {
    if (!confirm(`Your cart has items from ${REST_BY_ID[state.cart.restId].name}. Replace them with items from ${rest.name}?`)) return;
    state.cart = { restId: null, items: {} };
  }
  state.cart.restId = rest.id;
  state.cart.items[id] = (state.cart.items[id] || 0) + 1;
  persistCart();
  render();
}
function decItem(id) {
  if (!state.cart.items[id]) return;
  state.cart.items[id] -= 1;
  if (state.cart.items[id] <= 0) delete state.cart.items[id];
  if (!Object.keys(state.cart.items).length) state.cart.restId = null;
  persistCart();
  render();
}

function bill() {
  const items = cartItems();
  const sub = items.reduce((s, x) => s + x.item.price * x.qty, 0);
  const delivery = sub === 0 || sub >= FREE_DELIVERY_MIN ? 0 : DELIVERY_FEE;
  const eligible = COUPONS.filter(c => sub >= c.min).map(c => ({ c, off: Math.min(sub, c.calc(sub)) }));
  const best = eligible.reduce((a, b) => (!a || b.off > a.off ? b : a), null);
  let applied = null;
  if (state.coupon === 'auto') applied = best;
  else if (state.coupon !== 'none') applied = eligible.find(e => e.c.code === state.coupon) || null;
  const discount = applied ? applied.off : 0;
  const tax = (sub - discount) * GST_RATE;
  const platform = sub ? PLATFORM_FEE : 0;
  const total = sub - discount + delivery + tax + platform;
  return { items, sub, delivery, discount, applied, tax, platform, total };
}

function addOns() {
  const rest = REST_BY_ID[state.cart.restId];
  if (!rest) return [];
  const inCart = new Set(Object.keys(state.cart.items));
  const picks = [];
  for (const cat of ADDON_CATS) {
    if (rest.menu.some(d => d.cat === cat && inCart.has(d.id))) continue;
    const c = rest.menu.find(d => d.cat === cat && !inCart.has(d.id));
    if (c) picks.push(c);
  }
  return picks.slice(0, 4);
}

/* ---------- render: pieces ---------- */
const stepper = (id) => {
  const q = state.cart.items[id] || 0;
  return q
    ? `<div class="stepper"><button data-action="dec" data-arg="${id}" aria-label="Remove one">−</button><span>${q}</span><button data-action="inc" data-arg="${id}" aria-label="Add one">+</button></div>`
    : `<button class="add-btn" data-action="add" data-arg="${id}">ADD</button>`;
};

const vdot = veg => `<span class="vdot ${veg ? 'veg' : 'nonveg'}" title="${veg ? 'Veg' : 'Non-veg'}"></span>`;

function dishRow(it) {
  return `<div class="dish">
    <div>${vdot(it.veg)}${it.best ? '<span class="tag best">★ Bestseller</span>' : ''}
      <h4>${esc(it.name)}</h4><p class="price">${money(it.price)}</p></div>
    <div class="dish-side"><div class="dish-emoji">${it.emoji}</div>${stepper(it.id)}</div>
  </div>`;
}

function restCard(r) {
  const slot = SLOTS[slotKey()];
  return `<article class="rcard" role="button" aria-label="View menu at ${esc(r.name)}" data-action="openRest" data-arg="${r.id}" tabindex="0">
    <div class="banner" style="--h:${r.hue}"><span class="food-art" aria-hidden="true">${r.emoji}</span><span class="card-eyebrow">${esc(r.cuisines[0])} kitchen</span>${r.offers ? '<span class="ribbon">SAVE MORE · Coupons available</span>' : '<span class="ribbon">LOCAL FAVOURITE</span>'}</div>
    <div class="rbody">
      <div class="row"><h3>${esc(r.name)}</h3><span class="rating">★ ${r.rating}</span></div>
      <p class="muted">${esc(r.cuisines.join(', '))}</p>
      <div class="row muted"><span>⏱ ${r.eta} mins</span><span>${money(r.costForTwo)} for two</span></div>
      ${slotMatch(r) ? `<span class="tag">✨ Great for ${esc(slot.label.toLowerCase())}</span>` : ''}
      ${orderedBefore(r.id) ? '<span class="tag green">Ordered before</span>' : ''}
      ${r.pureVeg ? '<span class="tag green">Pure veg</span>' : ''}
    </div>
  </article>`;
}

function filterBar() {
  const f = state.filters;
  const chip = (k, label) => `<button class="chip ${f[k] ? 'on' : ''}" aria-pressed="${f[k]}" data-action="toggleFilter" data-arg="${k}">${label}</button>`;
  const opt = (v, l) => `<option value="${v}" ${state.sort === v ? 'selected' : ''}>${l}</option>`;
  return `<div class="filters">
    ${chip('veg', '🟢 Pure veg')}${chip('rating', `★ Rating ${TOP_MIN}+`)}${chip('fast', `⚡ Under ${FAST_MAX} mins`)}${chip('offers', '🏷️ Offers')}
    ${state.cuisine ? `<button class="chip on" data-action="cuisine" data-arg="">${esc(state.cuisine)} ✕</button>` : ''}
    <select data-change="sort" aria-label="Sort restaurants">
      ${opt('relevance', 'Sort: Smart relevance')}${opt('rating', 'Rating')}${opt('eta', 'Delivery time')}${opt('costLow', 'Cost: low to high')}${opt('costHigh', 'Cost: high to low')}
    </select>
  </div>`;
}

/* ---------- render: views ---------- */
function ordersSection() {
  if (!state.orders.length) return '';
  const rows = state.orders.slice(0, 3).map(o => {
    const done = stageOf(o) >= 3;
    return `<div class="result"><div class="row">
      <div><b>${o.emoji} ${esc(o.restName)}</b>
        <p class="muted">${esc(o.items.map(i => `${i.qty}× ${i.name}`).join(', '))}</p></div>
      <div>${done ? '' : `<button class="link" data-action="track" data-arg="${o.id}">Track</button> `}
        <button class="chip" data-action="reorder" data-arg="${o.id}">Reorder</button></div>
    </div></div>`;
  }).join('');
  return `<section><h2>Order again</h2>${rows}</section>`;
}

function homeView() {
  const sk = slotKey();
  const slot = SLOTS[sk];
  const list = sortRests(RESTAURANTS.filter(passesFilters));
  const slotOpt = (v, l) => `<option value="${v}" ${state.slot === v ? 'selected' : ''}>${l}</option>`;
  return `<section class="hero">
    <div class="hero-copy">
      <span class="eyebrow">YOUR NEIGHBOURHOOD. YOUR FAVOURITES.</span>
      <p class="hero-greeting">${slot.emoji} ${esc(slot.greet)} </p>
      <h1>Big cravings.<br>Little wait<span>.</span></h1>
      <p>From comforting classics to your next favourite.<br>Good food is always around the corner.</p>
      <button class="hero-cta" data-action="explore">Explore restaurants <span>↗</span></button>
      <div class="hero-proof"><span>★ 4.5+ local favourites</span><span>${RESTAURANTS.length} kitchens to explore</span></div>
    </div>
    <div class="hero-art" aria-hidden="true"><div class="orbit"></div><span class="hero-food">🍜</span><span class="floating-food food-one">🥟</span><span class="floating-food food-two">🥑</span><span class="art-sticker">Made fresh.<br><b>Delivered happy.</b></span></div>
  </section>
  <div class="discovery-note"><span><b>Picked for your ${esc(slot.label.toLowerCase())}</b> · Follow your cravings</span>
    <label class="sim">Meal time
      <select data-change="slot">${slotOpt('auto', 'Auto (now)')}${Object.entries(SLOTS).map(([k, s]) => slotOpt(k, s.label)).join('')}</select>
    </label>
  </div>
  <div class="meal-picks">
      <div class="chips">${slot.cuisines.map(c => `<button class="chip ${state.cuisine === c ? 'on' : ''}" data-action="cuisine" data-arg="${esc(c)}">${esc(c)}</button>`).join('')}</div>
  </div>
  <section class="offer-grid" aria-label="Food offers">
    <button class="offer-card offer-peach" data-action="offer" data-arg="WELCOME50"><span class="eyebrow">A LITTLE WELCOME TREAT</span><strong>First cravings? 50% off.</strong><span>Up to ₹100 · Min ₹199 · WELCOME50 →</span></button>
    <button class="offer-card offer-green" data-action="offer" data-arg="BIGFEAST"><span class="eyebrow">GOOD FOOD, BETTER TOGETHER</span><strong>Big feast. Bigger smiles.</strong><span>Save ₹150 · Min ₹699 · BIGFEAST →</span></button>
    <button class="offer-card offer-yellow" data-action="explore"><span class="eyebrow">STRAIGHT FROM LOCAL KITCHENS</span><strong>Your next favourite awaits.</strong><span>Explore ${RESTAURANTS.length} restaurants →</span></button>
  </section>
  ${ordersSection()}
  <section><div class="section-heading"><div><span class="eyebrow">A MOOD FOR EVERY MEAL</span><h2>What's on your mind?</h2></div><span class="muted">Pick a craving →</span></div><div class="cuisine-strip">
    ${CUISINES.map(([c, e]) => `<button class="cuisine ${state.cuisine === c ? 'on' : ''}" aria-pressed="${state.cuisine === c}" data-action="cuisine" data-arg="${esc(c)}"><div class="ico">${e}</div>${esc(c)}</button>`).join('')}
  </div></section>
  <section id="restaurants"><div class="section-heading"><div><span class="eyebrow">THE BEST OF YOUR NEIGHBOURHOOD</span><h2>${list.length} restaurants near you</h2></div><button class="link" data-action="resetFilters">Reset filters</button></div>${filterBar()}
    ${list.length ? `<div class="grid">${list.map(restCard).join('')}</div>` : '<div class="empty">No restaurants match these filters.</div>'}
  </section><section class="search-inspiration"><h2>A little help deciding?</h2><p class="muted">Search the way you speak. We'll find the good stuff.</p><div class="chips">${EXAMPLES.map(e => `<button class="chip example" data-action="setQuery" data-arg="${esc(e)}">${esc(e)} ↗</button>`).join('')}</div></section>${cartBar()}`;
}

function loginView() {
  return `<section class="login-layout">
    <div class="login-story"><span class="eyebrow">A TABLE FOR EVERY CRAVING</span><h1>Your favourites.<br>Your happy place.</h1><p>Comfort food, new discoveries and little celebrations. All in one delicious place.</p><div class="login-food" aria-hidden="true">🍕 🥗 🥟</div><div class="story-note"><b>${RESTAURANTS.length} local kitchens.</b><span>Endless reasons to order something good.</span></div></div>
    <div class="login-panel"><button class="back" data-action="leaveLogin">← Back to exploring</button><span class="eyebrow">WELCOME TO RUKAMINI</span><h2>Good to see you.</h2><p class="muted">Log in to continue your delicious journey.</p>
      <div class="demo-notice"><b>You're in a prototype</b><p>Demo login only, not real authentication. Use the sample details below, never your personal password.</p></div>
      <form id="loginForm">
        <label for="loginEmail">Email address</label><input id="loginEmail" name="email" type="email" autocomplete="off" placeholder="demo@rukmini.com" required>
        <label for="loginPassword">Demo password</label><div class="password-field"><input id="loginPassword" name="password" type="password" autocomplete="off" placeholder="Enter the demo password" required aria-describedby="demoCredentials"><button type="button" data-action="togglePassword" aria-label="Show password" aria-pressed="false">Show</button></div>
        <p id="loginError" class="form-error" role="alert" hidden></p>
        <button class="primary" type="submit">Log in & dig in →</button>
      </form>
      <div class="demo-credentials" id="demoCredentials"><span>Demo: <b>demo@rukmini.com</b> / <b>Demo@123</b></span><button class="link" data-action="fillDemo">Fill demo details</button></div>
      <div class="login-divider"><span>just looking around?</span></div><button class="guest-btn" data-action="leaveLogin">Continue as a guest →</button>
      <p class="login-fineprint">No account is created. Only a demo sign-in flag is kept for this browser tab; your password is never stored.</p>
    </div></section>`;
}

function showLogin(forCheckout = false) {
  if (state.view !== 'login') loginReturnView = state.view;
  checkoutAfterLogin = forCheckout;
  closeCart();
  hideSugg();
  state.view = 'login';
  render();
  window.scrollTo({ top: 0 });
  $('#loginEmail').focus({ preventScroll: true });
}

function leaveLogin() {
  state.view = loginReturnView;
  const reopenCart = checkoutAfterLogin;
  checkoutAfterLogin = false;
  render();
  if (reopenCart) openCart();
}

function setDemoUser(signedIn) {
  demoUser = signedIn;
  try {
    if (signedIn) sessionStorage.setItem('fl_demo_user', 'true');
    else sessionStorage.removeItem('fl_demo_user');
  } catch { toast('Session storage is unavailable. This change will not persist after reload.'); }
}

function restaurantView() {
  const r = REST_BY_ID[state.restId];
  const menu = r.menu.filter(d => !state.menuVeg || d.veg);
  const cats = [...new Set(menu.map(d => d.cat))];
  return `<button class="back" data-action="home">← Back to restaurants</button>
  <div class="rhead">
    <div><h1>${r.emoji} ${esc(r.name)}</h1>
      <p class="muted">${esc(r.cuisines.join(', '))} · ⏱ ${r.eta} mins · ${money(r.costForTwo)} for two</p></div>
    <div><span class="rating">★ ${r.rating}</span> <span class="muted">${esc(r.votes)} ratings</span></div>
    <label class="switch"><input type="checkbox" data-change="menuVeg" ${state.menuVeg ? 'checked' : ''}> Veg only</label>
  </div>
  ${cats.map(c => `<div class="cat"><h2>${esc(c)}</h2>${menu.filter(d => d.cat === c).map(dishRow).join('')}</div>`).join('')}
  ${cartBar()}`;
}

function searchView() {
  const p = parseQuery(state.query);
  let res = runSearch(p).filter(x => passesFilters(x.r));
  res = state.sort === 'relevance' ? res.sort((a, b) => b.score - a.score) : res.sort((a, b) => sorters[state.sort](a.r, b.r));
  const tags = [
    p.veg && 'Veg only', p.nonveg && 'Non-veg', p.max !== null && `Under ${money(p.max)}`,
    p.fast && `Under ${FAST_MAX} mins`, p.top && `Rating ${TOP_MIN}+`, ...p.terms
  ].filter(Boolean);
  const blocks = res.map(({ r, dishes }) => `<div class="result">
    <div class="result-head" role="button" tabindex="0" aria-label="View menu at ${esc(r.name)}" data-action="openRest" data-arg="${r.id}">
      <div class="mini" style="--h:${r.hue}">${r.emoji}</div>
      <div><b>${esc(r.name)}</b> <span class="rating">★ ${r.rating}</span>
        <p class="muted">${esc(r.cuisines.join(', '))} · ⏱ ${r.eta} mins</p></div>
    </div>
    ${dishes.slice(0, 4).map(dishRow).join('')}
  </div>`).join('');
  return `<section><h2>Results for “${esc(state.query)}”</h2>
    <div class="chips"><span class="muted">Understood as:</span>${tags.map(t => `<span class="chip on">${esc(t)}</span>`).join('') || '<span class="muted">everything</span>'}
      <button class="link" data-action="clearSearch">Clear</button></div>
    <br>${filterBar()}
    ${blocks || `<div class="empty"><p>Nothing found. Try one of these:</p><div class="chips">${EXAMPLES.map(e => `<button class="chip example" data-action="setQuery" data-arg="${esc(e)}">${esc(e)}</button>`).join('')}</div></div>`}
  </section>${cartBar()}`;
}

function cartBar() {
  const n = cartCount();
  if (!n) return '';
  const b = bill();
  return `<div class="cartbar" data-action="openCart" role="button" tabindex="0"><span>${n} item${n > 1 ? 's' : ''} · ${money(b.sub)}</span><span>View cart →</span></div>`;
}

/* ---------- render: cart drawer ---------- */
function renderCart() {
  const el = $('#drawer');
  const b = bill();
  const rest = REST_BY_ID[state.cart.restId];
  if (!b.items.length) {
    el.innerHTML = `<div class="drawer-head"><h3>Your cart</h3><button data-action="closeCart" aria-label="Close cart">✕</button></div>
      <div class="drawer-body"><div class="empty"><div style="font-size:56px">🛒</div><p>Your cart is empty.</p></div></div>`;
    return;
  }
  const toFree = FREE_DELIVERY_MIN - b.sub;
  const pct = Math.min(100, Math.round((b.sub / FREE_DELIVERY_MIN) * 100));
  const nextCoupon = COUPONS.filter(c => b.sub < c.min).sort((a, c) => a.min - c.min)[0];
  el.innerHTML = `<div class="drawer-head"><div><h3>${rest.emoji} ${esc(rest.name)}</h3><p class="muted">${b.items.length} item type(s)</p></div>
      <button data-action="closeCart" aria-label="Close cart">✕</button></div>
    <div class="drawer-body">
      ${b.items.map(({ item, qty }) => `<div class="line"><span>${vdot(item.veg)}${esc(item.name)}</span>
        <span>${stepper(item.id)}</span><span>${money(item.price * qty)}</span></div>`).join('')}
      <div class="nudge">${toFree > 0
        ? `Add <b>${money(toFree)}</b> more for <b>free delivery</b><div class="bar"><i style="width:${pct}%"></i></div>`
        : '🎉 You unlocked <b>free delivery</b>'}
        ${nextCoupon && state.coupon !== 'none' ? `<br>Add ${money(nextCoupon.min - b.sub)} more to unlock <b>${nextCoupon.code}</b>` : ''}</div>
      ${addOnsHtml()}
      <h3 style="margin-top:18px">Offers</h3>
      ${COUPONS.map(c => {
        const ok = b.sub >= c.min;
        const on = b.applied && b.applied.c.code === c.code;
        return `<div class="coupon ${ok ? '' : 'off'}"><span><b>${c.code}</b> · ${c.desc} (min ${money(c.min)})</span>
          ${on ? '<button class="link" data-action="removeCoupon">Remove</button>' : ok ? `<button class="link" data-action="applyCoupon" data-arg="${c.code}">Apply</button>` : '<span class="muted">Locked</span>'}</div>`;
      }).join('')}
      <div class="coupon-in"><input id="couponIn" placeholder="Enter coupon code" maxlength="20" aria-label="Coupon code"><button class="chip" data-action="applyTyped">Apply</button></div>
      ${state.coupon === 'auto' && b.applied ? '<p class="muted">Best offer auto-applied.</p>' : ''}
      <h3 style="margin-top:18px">Bill details</h3>
      <div class="bill">
        <div class="line"><span>Item total</span><span>${money(b.sub)}</span></div>
        ${b.discount ? `<div class="line save"><span>Discount (${b.applied.c.code})</span><span>−${money(b.discount)}</span></div>` : ''}
        <div class="line"><span>Delivery fee</span><span>${b.delivery ? money(b.delivery) : 'FREE'}</span></div>
        <div class="line"><span>GST (5%)</span><span>${money(b.tax)}</span></div>
        <div class="line"><span>Platform fee</span><span>${money(b.platform)}</span></div>
        <div class="line total"><span>To pay</span><span>${money(b.total)}</span></div>
      </div>
      <button class="link" data-action="clearCart">Clear cart</button>
    </div>
    <div class="drawer-foot"><button class="primary" data-action="place">Place order · ${money(b.total)}</button></div>`;
}

function addOnsHtml() {
  const picks = addOns();
  if (!picks.length) return '';
  return `<h3 style="margin-top:18px">Complete your meal</h3><div class="addons">${picks.map(d =>
    `<div class="addon"><div class="dish-emoji">${d.emoji}</div><div>${esc(d.name)}</div><div class="price">${money(d.price)}</div>
     <button class="add-btn" data-action="add" data-arg="${d.id}">ADD</button></div>`).join('')}</div>`;
}

/* ---------- orders & tracking ---------- */
const stageOf = o => Math.min(3, Math.floor((Date.now() - o.placedAt) / STAGE_MS));
let trackTimer = null;

function placeOrder() {
  const b = bill();
  if (!b.items.length) return;
  const rest = REST_BY_ID[state.cart.restId];
  const order = {
    id: 'FL' + String(Date.now()).slice(-6), restId: rest.id, restName: rest.name, emoji: rest.emoji,
    items: b.items.map(({ item, qty }) => ({ id: item.id, name: item.name, qty, price: item.price, veg: item.veg })),
    total: Math.round(b.total), saved: b.discount, placedAt: Date.now(), eta: rest.eta,
    rider: RIDERS[Math.floor(Math.random() * RIDERS.length)]
  };
  state.orders = [order, ...state.orders].slice(0, 10);
  save('fl_orders', state.orders);
  state.cart = { restId: null, items: {} };
  state.coupon = 'auto';
  persistCart();
  closeCart();
  render();
  openTracking(order.id);
}

function openTracking(id) {
  state.trackId = id;
  renderTracking();
  clearInterval(trackTimer);
  trackTimer = setInterval(() => {
    const o = state.orders.find(x => x.id === state.trackId);
    renderTracking();
    if (!o || stageOf(o) >= 3) { clearInterval(trackTimer); render(); }
  }, 1000);
}

function closeTracking() {
  clearInterval(trackTimer);
  state.trackId = null;
  $('#modal').hidden = true;
}

function renderTracking() {
  const o = state.orders.find(x => x.id === state.trackId);
  const modal = $('#modal');
  if (!o) { modal.hidden = true; return; }
  const st = stageOf(o);
  const elapsed = Date.now() - o.placedAt;
  const remaining = Math.ceil(o.eta * (1 - Math.min(1, elapsed / (3 * STAGE_MS))));
  modal.hidden = false;
  modal.innerHTML = `<div class="sheet" role="dialog" aria-label="Order tracking">
    <div class="row"><h3>Order #${esc(o.id)}</h3><button data-action="closeModal" aria-label="Close">✕</button></div>
    <p class="muted">${o.emoji} ${esc(o.restName)}</p>
    <p class="eta">${st >= 3 ? 'Delivered' : `Arriving in ${remaining} min`}</p>
    <p class="muted">(Demo speed: each stage takes ${STAGE_MS / 1000}s)</p>
    <ul class="stages">${STAGES.map((s, i) => `<li class="${i < st ? 'done' : ''} ${i === st ? 'now' : ''}">
      <span class="ico">${s.i}</span><div>${esc(s.k)}<p class="muted">${i === 2 ? `${esc(o.rider)} · ${esc(s.d)}` : esc(s.d)}</p></div></li>`).join('')}</ul>
    <p class="muted">${esc(o.items.map(i => `${i.qty}× ${i.name}`).join(', '))}</p>
    <div class="row"><b>Paid ${money(o.total)}</b>${o.saved ? `<span class="save">You saved ${money(o.saved)}</span>` : ''}</div>
  </div>`;
}

function reorder(id) {
  const o = state.orders.find(x => x.id === id);
  if (!o) return;
  const items = {};
  o.items.forEach(i => { if (ITEMS.has(i.id)) items[i.id] = i.qty; });
  if (cartCount() && !confirm('Replace your current cart with this previous order?')) return;
  state.cart = { restId: o.restId, items };
  state.coupon = 'auto';
  persistCart();
  render();
  openCart();
}

/* ---------- main render ---------- */
function render() {
  const app = $('#app');
  if (state.view === 'login') app.innerHTML = loginView();
  else if (state.view === 'rest' && state.restId) app.innerHTML = restaurantView();
  else if (state.query.trim()) app.innerHTML = searchView();
  else app.innerHTML = homeView();
  renderCart();
  const account = $('#accountBtn');
  account.textContent = demoUser ? 'Log out' : 'Log in';
  account.dataset.action = demoUser ? 'logout' : 'login';
  const n = cartCount();
  const badge = $('#cartCount');
  badge.textContent = n;
  badge.hidden = n === 0;
}

function openCart() { renderCart(); $('#drawer').classList.add('open'); $('#scrim').hidden = false; }
function closeCart() { $('#drawer').classList.remove('open'); $('#scrim').hidden = true; }

let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.hidden = true; }, 2200);
}

function setQuery(text) {
  state.query = text;
  state.view = 'home';
  $('#q').value = text;
  hideSugg();
  render();
  window.scrollTo({ top: 0 });
}

function hideSugg() { $('#sugg').hidden = true; }

/* ---------- events ---------- */
const actions = {
  explore: () => {
    actions.home();
    $('#restaurants').scrollIntoView({ behavior: 'smooth', block: 'start' });
  },
  resetFilters: () => {
    state.filters = { veg: false, rating: false, fast: false, offers: false };
    state.cuisine = null;
    state.sort = 'relevance';
    render();
  },
  offer: code => {
    state.coupon = code;
    if (cartCount()) { render(); openCart(); }
    else actions.explore();
    const coupon = COUPONS.find(c => c.code === code);
    toast(bill().sub >= coupon.min ? code + ' applied' : code + ' selected. Add ' + money(coupon.min - bill().sub) + ' to unlock.');
  },
  login: () => showLogin(),
  logout: () => { setDemoUser(false); render(); toast('You are signed out of the demo.'); },
  leaveLogin,
  fillDemo: () => {
    $('#loginEmail').value = 'demo@rukmini.com';
    $('#loginPassword').value = 'Demo@123';
    $('#loginEmail').focus();
  },
  togglePassword: () => {
    const input = $('#loginPassword');
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    const button = $('[data-action="togglePassword"]');
    button.textContent = show ? 'Hide' : 'Show';
    button.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    button.setAttribute('aria-pressed', String(show));
  },
  home: () => { state.view = 'home'; state.restId = null; state.query = ''; $('#q').value = ''; render(); },
  openRest: id => { state.view = 'rest'; state.restId = id; state.menuVeg = false; render(); window.scrollTo({ top: 0 }); },
  openCart,
  closeCart,
  add: addItem,
  inc: addItem,
  dec: decItem,
  clearCart: () => { state.cart = { restId: null, items: {} }; persistCart(); render(); },
  cuisine: c => { state.cuisine = c && state.cuisine !== c ? c : null; render(); },
  toggleFilter: k => { state.filters[k] = !state.filters[k]; render(); },
  setQuery,
  clearSearch: () => setQuery(''),
  applyCoupon: code => { state.coupon = code; render(); toast(`${code} applied`); },
  removeCoupon: () => { state.coupon = 'none'; render(); },
  applyTyped: () => {
    const code = $('#couponIn').value.trim().toUpperCase();
    const c = COUPONS.find(x => x.code === code);
    if (!c) { toast('Invalid coupon code'); return; }
    if (bill().sub < c.min) { toast(`Add ${money(c.min - bill().sub)} more to use ${code}`); return; }
    state.coupon = code;
    render();
    toast(`${code} applied`);
  },
  place: () => { if (demoUser) placeOrder(); else showLogin(true); },
  track: openTracking,
  reorder,
  closeModal: closeTracking
};

document.addEventListener('submit', e => {
  if (e.target.id !== 'loginForm') return;
  e.preventDefault();
  if ($('#loginEmail').value.trim().toLowerCase() !== 'demo@rukmini.com' || $('#loginPassword').value !== 'Demo@123') {
    const error = $('#loginError');
    error.textContent = 'Use the sample demo details below to sign in.';
    error.hidden = false;
    return;
  }
  setDemoUser(true);
  leaveLogin();
  toast('Demo login successful. No real order or payment will be made.');
});

document.addEventListener('click', e => {
  const el = e.target.closest('[data-action]');
  if (!el) { if (!e.target.closest('.search')) hideSugg(); return; }
  const fn = actions[el.dataset.action];
  if (fn) fn(el.dataset.arg);
});

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') { closeCart(); closeTracking(); hideSugg(); }
  if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('[role="button"], .rcard')) {
    e.preventDefault();
    e.target.click();
  }
});

document.addEventListener('change', e => {
  const k = e.target.dataset.change;
  if (!k) return;
  if (k === 'sort') state.sort = e.target.value;
  if (k === 'slot') state.slot = e.target.value;
  if (k === 'menuVeg') state.menuVeg = e.target.checked;
  render();
});

$('#q').addEventListener('input', e => {
  state.query = e.target.value;
  state.view = 'home';
  const s = suggest(state.query);
  const box = $('#sugg');
  box.innerHTML = s.map(v => `<button data-action="setQuery" data-arg="${esc(v.q)}">${v.icon} ${esc(v.label)}</button>`).join('');
  box.hidden = !s.length;
  render();
});

$('#q').addEventListener('keydown', e => { if (e.key === 'Enter') hideSugg(); });

render();
