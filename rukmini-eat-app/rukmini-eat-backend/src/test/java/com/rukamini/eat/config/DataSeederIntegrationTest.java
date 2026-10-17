package com.rukamini.eat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rukamini.eat.repository.MenuItemRepository;
import com.rukamini.eat.repository.RestaurantRepository;
import com.rukamini.eat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seeder-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DataSeederIntegrationTest {

    @Autowired
    private DataSeeder seeder;
    @Autowired
    private UserRepository users;
    @Autowired
    private RestaurantRepository restaurants;
    @Autowired
    private MenuItemRepository menuItems;

    @Test
    void seedingIsIdempotentAndCreatesTheDocumentedData() {
        long userCount = users.count();
        long restaurantCount = restaurants.count();
        long menuCount = menuItems.count();
        assertEquals(2, userCount);
        assertEquals(8, restaurantCount);
        assertTrue(menuCount >= 8 * 5);

        seeder.run();

        assertEquals(userCount, users.count());
        assertEquals(restaurantCount, restaurants.count());
        assertEquals(menuCount, menuItems.count());
    }

    @Test
    void demoPasswordsAreStoredHashed() {
        for (String email : new String[] {"demo@rukmini.com", "admin@rukmini.com"}) {
            String hash = users.findByEmail(email).orElseThrow().getPasswordHash();
            assertTrue(hash.startsWith("$2"), "bcrypt hash expected");
            assertFalse(hash.contains("Demo@123") || hash.contains("Admin@123"));
        }
    }
}
