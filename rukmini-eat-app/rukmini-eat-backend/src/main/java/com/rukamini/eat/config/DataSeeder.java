package com.rukamini.eat.config;

import com.rukamini.eat.domain.MenuItem;
import com.rukamini.eat.domain.Restaurant;
import com.rukamini.eat.domain.Role;
import com.rukamini.eat.domain.User;
import com.rukamini.eat.repository.RestaurantRepository;
import com.rukamini.eat.repository.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads demo users and restaurants on first start. Demo credentials are for local use only. */
@Component
@Profile("local")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, RestaurantRepository restaurants, PasswordEncoder encoder) {
        this.users = users;
        this.restaurants = restaurants;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("Demo Customer", "demo@rukmini.com", "Demo@123", Role.CUSTOMER);
        seedUser("Admin", "admin@rukmini.com", "Admin@123", Role.ADMIN);
        if (restaurants.count() == 0) {
            seedRestaurants();
        }
    }

    private void seedUser(String name, String email, String password, Role role) {
        if (!users.existsByEmail(email)) {
            users.save(new User(name, email, encoder.encode(password), role));
        }
    }

    private void seedRestaurants() {
        Restaurant biryani = new Restaurant("Spice Route Biryani", "Biryani,North Indian,Mughlai", 4.5, 32, 600, false, "🍛",
                "Slow-cooked dum biryani and Mughlai classics.");
        item(biryani, "Hyderabadi Chicken Dum Biryani", "Biryani", 289, false, true, "🍗");
        item(biryani, "Mutton Biryani", "Biryani", 389, false, false, "🍖");
        item(biryani, "Paneer Tikka Biryani", "Biryani", 249, true, false, "🧀");
        item(biryani, "Chicken 65", "Starters", 199, false, false, "🍗");
        item(biryani, "Mirchi Ka Salan", "Sides", 79, true, false, "🌶️");
        item(biryani, "Masala Soda", "Drinks", 59, true, false, "🥤");
        item(biryani, "Double Ka Meetha", "Desserts", 99, true, false, "🍮");

        Restaurant pizza = new Restaurant("Pizza Piazza", "Pizza,Italian,Fast Food", 4.3, 25, 450, false, "🍕",
                "Wood-fired style pizzas, sides and desserts.");
        item(pizza, "Margherita Pizza", "Pizzas", 199, true, true, "🍕");
        item(pizza, "Farmhouse Pizza", "Pizzas", 299, true, false, "🍕");
        item(pizza, "Pepperoni Pizza", "Pizzas", 349, false, false, "🍕");
        item(pizza, "Garlic Bread", "Sides", 119, true, false, "🥖");
        item(pizza, "Peri Peri Fries", "Sides", 129, true, false, "🍟");
        item(pizza, "Cola", "Drinks", 60, true, false, "🥤");
        item(pizza, "Choco Lava Cake", "Desserts", 99, true, false, "🍫");

        Restaurant burger = new Restaurant("Burger Barn", "Burgers,American,Fast Food", 4.2, 22, 350, false, "🍔",
                "Juicy burgers, crispy fries and shakes.");
        item(burger, "Classic Veg Burger", "Burgers", 129, true, false, "🍔");
        item(burger, "Crispy Chicken Burger", "Burgers", 179, false, true, "🍔");
        item(burger, "Double Patty Beast", "Burgers", 249, false, false, "🍔");
        item(burger, "Fries", "Sides", 99, true, false, "🍟");
        item(burger, "Onion Rings", "Sides", 109, true, false, "🧅");
        item(burger, "Cold Coffee", "Drinks", 119, true, false, "🥤");
        item(burger, "Brownie Sundae", "Desserts", 149, true, false, "🍨");

        Restaurant dosa = new Restaurant("Dosa Darbar", "South Indian,Breakfast", 4.6, 20, 250, true, "🥞",
                "Pure vegetarian South Indian tiffin.");
        item(dosa, "Masala Dosa", "Dosas", 99, true, true, "🥞");
        item(dosa, "Ghee Roast Dosa", "Dosas", 119, true, false, "🥞");
        item(dosa, "Idli (3 pcs)", "Tiffin", 69, true, false, "🍚");
        item(dosa, "Medu Vada", "Tiffin", 79, true, false, "🍩");
        item(dosa, "Filter Coffee", "Drinks", 49, true, false, "☕");
        item(dosa, "Kesari Bath", "Desserts", 59, true, false, "🍮");

        Restaurant wok = new Restaurant("Wok This Way", "Chinese,Asian", 4.1, 30, 450, false, "🥡",
                "Indo-Chinese noodles, rice and starters.");
        item(wok, "Veg Hakka Noodles", "Noodles & Rice", 169, true, false, "🍜");
        item(wok, "Chicken Schezwan Fried Rice", "Noodles & Rice", 199, false, false, "🍚");
        item(wok, "Chilli Chicken", "Starters", 229, false, true, "🍗");
        item(wok, "Gobi Manchurian", "Starters", 179, true, false, "🥦");
        item(wok, "Hot & Sour Soup", "Sides", 99, true, false, "🍲");
        item(wok, "Iced Lemon Tea", "Drinks", 79, true, false, "🧋");

        Restaurant bowl = new Restaurant("Green Bowl", "Healthy,Salads,Continental", 4.4, 24, 500, true, "🥗",
                "Fresh bowls, salads and cold-pressed juices.");
        item(bowl, "Quinoa Power Bowl", "Bowls", 249, true, true, "🥗");
        item(bowl, "Paneer Tikka Protein Bowl", "Bowls", 279, true, false, "🥙");
        item(bowl, "Greek Salad", "Salads", 199, true, false, "🥒");
        item(bowl, "Hummus & Pita", "Sides", 139, true, false, "🫓");
        item(bowl, "Detox Green Juice", "Drinks", 129, true, false, "🥬");
        item(bowl, "Chia Pudding", "Desserts", 149, true, false, "🍮");

        Restaurant sweets = new Restaurant("Sweet Tooth Studio", "Desserts,Bakery", 4.7, 27, 400, true, "🍰",
                "Cakes, sundaes and fresh bakes.");
        item(sweets, "Belgian Chocolate Truffle Slice", "Desserts", 129, true, true, "🍰");
        item(sweets, "Red Velvet Jar", "Desserts", 149, true, false, "🧁");
        item(sweets, "Death by Chocolate Sundae", "Desserts", 189, true, false, "🍨");
        item(sweets, "Walnut Brownie", "Bakery", 99, true, false, "🍫");
        item(sweets, "Hot Chocolate", "Drinks", 129, true, false, "☕");

        Restaurant tandoor = new Restaurant("Tandoor Tales", "North Indian,Tandoor,Mughlai", 4.4, 35, 700, false, "🥘",
                "Rich curries and charcoal-grilled tandoor.");
        item(tandoor, "Butter Chicken", "Mains", 329, false, true, "🍗");
        item(tandoor, "Dal Makhani", "Mains", 249, true, false, "🥘");
        item(tandoor, "Paneer Butter Masala", "Mains", 289, true, false, "🧀");
        item(tandoor, "Butter Naan", "Breads", 59, true, false, "🫓");
        item(tandoor, "Garlic Naan", "Breads", 69, true, false, "🫓");
        item(tandoor, "Hara Bhara Kebab", "Starters", 219, true, false, "🥬");
        item(tandoor, "Sweet Lassi", "Drinks", 89, true, false, "🥛");

        restaurants.saveAll(java.util.List.of(biryani, pizza, burger, dosa, wok, bowl, sweets, tandoor));
    }

    private void item(Restaurant r, String name, String category, int price, boolean veg, boolean best, String emoji) {
        r.addMenuItem(new MenuItem(name, category, BigDecimal.valueOf(price), veg, best, emoji));
    }
}
