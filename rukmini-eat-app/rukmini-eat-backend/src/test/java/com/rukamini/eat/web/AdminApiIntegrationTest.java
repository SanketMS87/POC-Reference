package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AdminApiIntegrationTest extends ApiTestSupport {

    private String restaurantJson(String name) {
        return "{\"name\":\"" + name + "\",\"cuisines\":\"North Indian, Tandoor\",\"rating\":4.5,"
                + "\"etaMinutes\":30,\"costForTwo\":500,\"pureVeg\":false,\"emoji\":\"\","
                + "\"description\":\"Created by a test\"}";
    }

    private String menuItemJson(String name, String category, String price) {
        return "{\"name\":\"" + name + "\",\"category\":\"" + category + "\",\"price\":" + price
                + ",\"veg\":true,\"bestseller\":false,\"emoji\":\"\"}";
    }

    private JsonNode createRestaurant(String admin, String name) throws Exception {
        var reply = call("POST", "/api/admin/restaurants", restaurantJson(name), admin);
        assertEquals(201, reply.status(), reply.body());
        return reply.json();
    }

    @Test
    void adminCreatesARestaurantThatCustomersCanFind() throws Exception {
        String admin = adminToken();
        String name = "Test Kitchen " + UUID.randomUUID();
        JsonNode created = createRestaurant(admin, "  " + name + "  ");

        assertEquals(name, created.path("name").asText());
        assertEquals("North Indian", created.path("cuisines").path(0).asText());
        assertEquals("Tandoor", created.path("cuisines").path(1).asText());
        assertEquals(4.5, created.path("rating").asDouble());
        assertFalse(created.path("emoji").asText().isBlank());
        assertTrue(created.path("id").asLong() > 0);

        String customer = newCustomerToken();
        assertEquals(created.path("id").asLong(), restaurantNamed(customer, name).path("id").asLong());
        var detail = call("GET", "/api/restaurants/" + created.path("id").asLong(), null, customer);
        assertEquals(200, detail.status());
        assertTrue(detail.json().path("menu").isEmpty());
    }

    @Test
    void menuItemsAddedByAdminAppearInDetailAndCanBeOrdered() throws Exception {
        String admin = adminToken();
        JsonNode restaurant = createRestaurant(admin, "Menu Test " + UUID.randomUUID());
        long restaurantId = restaurant.path("id").asLong();

        var added = call("POST", "/api/admin/restaurants/" + restaurantId + "/menu-items",
                menuItemJson("  Paneer Tikka  ", "Starters", "249.00"), admin);
        assertEquals(201, added.status(), added.body());
        JsonNode item = added.json();
        assertEquals("Paneer Tikka", item.path("name").asText());
        assertEquals("Starters", item.path("category").asText());
        assertEquals(0, new java.math.BigDecimal("249.00").compareTo(item.path("price").decimalValue()));
        assertTrue(item.path("veg").asBoolean());
        assertFalse(item.path("emoji").asText().isBlank());

        String customer = newCustomerToken();
        JsonNode detail = call("GET", "/api/restaurants/" + restaurantId, null, customer).json();
        assertEquals(1, detail.path("menu").size());

        var order = call("POST", "/api/orders", orderBody(item.path("id").asLong(), 2), customer);
        assertEquals(201, order.status(), order.body());
        assertEquals(0, new java.math.BigDecimal("498").compareTo(order.json().path("subtotal").decimalValue()));
        assertEquals(restaurant.path("name").asText(), order.json().path("restaurantName").asText());
    }

    @Test
    void restaurantValidationRejectsOutOfRangeValues() throws Exception {
        String admin = adminToken();
        String valid = restaurantJson("Validation " + UUID.randomUUID());
        assertEquals(201, call("POST", "/api/admin/restaurants", valid, admin).status());
        String[][] invalid = {
                {"\"rating\":4.5", "\"rating\":5.1"},
                {"\"rating\":4.5", "\"rating\":-0.1"},
                {"\"etaMinutes\":30", "\"etaMinutes\":4"},
                {"\"etaMinutes\":30", "\"etaMinutes\":181"},
                {"\"costForTwo\":500", "\"costForTwo\":-1"},
                {"\"cuisines\":\"North Indian, Tandoor\"", "\"cuisines\":\"\""},
                {"\"emoji\":\"\"", "\"emoji\":\"123456789\""},
        };
        for (String[] change : invalid) {
            var reply = call("POST", "/api/admin/restaurants",
                    restaurantJson("Validation " + UUID.randomUUID()).replace(change[0], change[1]), admin);
            assertEquals(400, reply.status(), change[1]);
        }
        assertEquals(400, call("POST", "/api/admin/restaurants", "{}", admin).status());
        assertEquals(400, call("POST", "/api/admin/restaurants",
                restaurantJson("   "), admin).status());
    }

    @Test
    void menuItemValidationRejectsBadInput() throws Exception {
        String admin = adminToken();
        long id = createRestaurant(admin, "Item Validation " + UUID.randomUUID()).path("id").asLong();
        String path = "/api/admin/restaurants/" + id + "/menu-items";
        assertEquals(400, call("POST", path, menuItemJson("", "Starters", "10"), admin).status());
        assertEquals(400, call("POST", path, menuItemJson("Dish", "", "10"), admin).status());
        assertEquals(400, call("POST", path, menuItemJson("Dish", "Starters", "0.99"), admin).status());
        assertEquals(400, call("POST", path, menuItemJson("Dish", "Starters", "-5"), admin).status());
        assertEquals(400, call("POST", path, "{}", admin).status());
        assertEquals(201, call("POST", path, menuItemJson("Dish", "Starters", "1.00"), admin).status());
    }

    @Test
    void addingAMenuItemToAnUnknownRestaurantIsNotFound() throws Exception {
        var reply = call("POST", "/api/admin/restaurants/999999/menu-items",
                menuItemJson("Dish", "Starters", "10"), adminToken());
        assertEquals(404, reply.status());
        assertEquals("Restaurant not found", reply.message());
    }

    @Test
    void customersAndAnonymousUsersCannotUseAdminEndpoints() throws Exception {
        String customer = newCustomerToken();
        String name = "Forbidden " + UUID.randomUUID();
        assertEquals(403, call("POST", "/api/admin/restaurants", restaurantJson(name), customer).status());
        assertEquals(403, call("POST", "/api/admin/restaurants/1/menu-items",
                menuItemJson("Dish", "Starters", "10"), customer).status());
        assertEquals(401, call("POST", "/api/admin/restaurants", restaurantJson(name), null).status());
        assertEquals(401, call("POST", "/api/admin/restaurants/1/menu-items",
                menuItemJson("Dish", "Starters", "10"), null).status());
        for (JsonNode r : restaurants(customer, "")) {
            assertFalse(name.equals(r.path("name").asText()), "forbidden request must not create data");
        }
    }

    @Test
    void adminsCanAlsoUseCustomerEndpoints() throws Exception {
        String admin = adminToken();
        assertEquals(200, call("GET", "/api/restaurants", null, admin).status());
        assertEquals(200, call("GET", "/api/orders", null, admin).status());
        assertEquals(200, call("GET", "/api/integrations/providers", null, admin).status());
    }
}
