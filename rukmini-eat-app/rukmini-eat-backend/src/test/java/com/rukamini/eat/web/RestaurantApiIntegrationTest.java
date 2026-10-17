package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class RestaurantApiIntegrationTest extends ApiTestSupport {

    private String token;

    @BeforeEach
    void signIn() throws Exception {
        token = newCustomerToken();
    }

    private List<String> names(JsonNode list) {
        List<String> names = new ArrayList<>();
        list.forEach(r -> names.add(r.path("name").asText()));
        return names;
    }

    @Test
    void listContainsTheSeededRestaurants() throws Exception {
        List<String> names = names(restaurants(token, ""));
        for (String expected : List.of("Spice Route Biryani", "Pizza Piazza", "Burger Barn", "Dosa Darbar",
                "Wok This Way", "Green Bowl", "Sweet Tooth Studio", "Tandoor Tales")) {
            assertTrue(names.contains(expected), expected);
        }
    }

    @Test
    void summariesExposeSplitCuisinesAndNoMenu() throws Exception {
        JsonNode biryani = restaurantNamed(token, "Spice Route Biryani");
        List<String> cuisines = new ArrayList<>();
        biryani.path("cuisines").forEach(c -> cuisines.add(c.asText()));
        assertEquals(List.of("Biryani", "North Indian", "Mughlai"), cuisines);
        assertEquals(4.5, biryani.path("rating").asDouble());
        assertEquals(32, biryani.path("etaMinutes").asInt());
        assertEquals(600, biryani.path("costForTwo").asInt());
        assertFalse(biryani.path("pureVeg").asBoolean());
        assertTrue(biryani.path("menu").isMissingNode());
    }

    @Test
    void vegOnlyReturnsOnlyPureVegRestaurants() throws Exception {
        JsonNode result = restaurants(token, "?vegOnly=true");
        result.forEach(r -> assertTrue(r.path("pureVeg").asBoolean(), r.path("name").asText()));
        assertTrue(names(result).contains("Dosa Darbar"));
        assertFalse(names(result).contains("Burger Barn"));
    }

    @Test
    void topRatedIncludesTheBoundaryAndExcludesLowerRatings() throws Exception {
        JsonNode result = restaurants(token, "?topRated=true");
        result.forEach(r -> assertTrue(r.path("rating").asDouble() >= 4.3, r.path("name").asText()));
        assertTrue(names(result).contains("Pizza Piazza"));
        assertFalse(names(result).contains("Wok This Way"));
    }

    @Test
    void fastIncludesTheBoundaryAndExcludesSlowerRestaurants() throws Exception {
        JsonNode result = restaurants(token, "?fast=true");
        result.forEach(r -> assertTrue(r.path("etaMinutes").asInt() <= 25, r.path("name").asText()));
        assertTrue(names(result).contains("Pizza Piazza"));
        assertFalse(names(result).contains("Spice Route Biryani"));
    }

    @Test
    void cuisineFilterIsCaseInsensitiveSubstring() throws Exception {
        JsonNode result = restaurants(token, "?cuisine=BIRYANI");
        assertTrue(names(result).contains("Spice Route Biryani"));
        result.forEach(r -> assertTrue(r.path("cuisines").toString().toLowerCase().contains("biryani")));
        assertTrue(restaurants(token, "?cuisine=klingon").isEmpty());
    }

    @Test
    void searchMatchesNameCuisineAndDishNames() throws Exception {
        assertEquals(List.of("Pizza Piazza"), names(restaurants(token, "?q=margherita")));
        assertTrue(names(restaurants(token, "?q=burger")).contains("Burger Barn"));
        assertTrue(names(restaurants(token, "?q=%20%20TANDOOR%20")).contains("Tandoor Tales"));
        assertTrue(restaurants(token, "?q=zzz-no-such-thing").isEmpty());
    }

    @Test
    void filtersCanBeCombined() throws Exception {
        JsonNode result = restaurants(token, "?vegOnly=true&topRated=true&fast=true&sort=eta");
        result.forEach(r -> {
            assertTrue(r.path("pureVeg").asBoolean());
            assertTrue(r.path("rating").asDouble() >= 4.3);
            assertTrue(r.path("etaMinutes").asInt() <= 25);
        });
        assertTrue(names(result).contains("Dosa Darbar"));
    }

    @Test
    void sortingOrdersResults() throws Exception {
        assertSorted(restaurants(token, "?sort=rating"), "rating", false);
        assertSorted(restaurants(token, "?sort=eta"), "etaMinutes", true);
        assertSorted(restaurants(token, "?sort=costLow"), "costForTwo", true);
        assertSorted(restaurants(token, "?sort=costHigh"), "costForTwo", false);
    }

    private void assertSorted(JsonNode list, String field, boolean ascending) {
        double previous = ascending ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (JsonNode r : list) {
            double value = r.path(field).asDouble();
            assertTrue(ascending ? value >= previous : value <= previous, field + " not sorted");
            previous = value;
        }
    }

    @Test
    void unknownSortFallsBackToRelevanceAndBlankFiltersAreIgnored() throws Exception {
        assertEquals(restaurants(token, "").size(), restaurants(token, "?sort=nonsense").size());
        assertEquals(restaurants(token, "").size(), restaurants(token, "?q=&cuisine=&sort=").size());
    }

    @Test
    void detailReturnsMenuItemsWithPricesAndFlags() throws Exception {
        JsonNode dosa = restaurantNamed(token, "Dosa Darbar");
        var reply = call("GET", "/api/restaurants/" + dosa.path("id").asLong(), null, token);
        assertEquals(200, reply.status());
        JsonNode detail = reply.json();
        assertEquals("Dosa Darbar", detail.path("name").asText());
        assertFalse(detail.path("menu").isEmpty());
        JsonNode masala = menuItemNamed(detail, "Masala Dosa");
        assertEquals(99, masala.path("price").asInt());
        assertTrue(masala.path("veg").asBoolean());
        assertTrue(masala.path("bestseller").asBoolean());
        assertEquals("Dosas", masala.path("category").asText());
    }

    @Test
    void unknownRestaurantIsNotFoundAndMalformedIdIsABadRequest() throws Exception {
        var missing = call("GET", "/api/restaurants/999999", null, token);
        assertEquals(404, missing.status());
        assertEquals("Restaurant not found", missing.message());
        assertEquals(400, call("GET", "/api/restaurants/abc", null, token).status());
    }

    @Test
    void restaurantEndpointsRequireAuthentication() throws Exception {
        assertEquals(401, call("GET", "/api/restaurants", null, null).status());
        assertEquals(401, call("GET", "/api/restaurants/1", null, null).status());
    }
}
