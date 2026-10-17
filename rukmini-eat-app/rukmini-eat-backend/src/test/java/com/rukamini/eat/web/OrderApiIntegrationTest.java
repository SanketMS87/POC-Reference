package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class OrderApiIntegrationTest extends ApiTestSupport {

    private String token;
    private JsonNode pizza;
    private JsonNode burger;

    @BeforeEach
    void setUp() throws Exception {
        token = newCustomerToken();
        pizza = detailOf(token, restaurantNamed(token, "Pizza Piazza"));
        burger = detailOf(token, restaurantNamed(token, "Burger Barn"));
    }

    private long id(JsonNode detail, String item) {
        return menuItemNamed(detail, item).path("id").asLong();
    }

    private void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void ordersBelowTheThresholdPayDeliveryFee() throws Exception {
        var reply = call("POST", "/api/orders", orderBody(id(pizza, "Margherita Pizza"), 1), token);
        assertEquals(201, reply.status());
        JsonNode order = reply.json();
        assertEquals("Pizza Piazza", order.path("restaurantName").asText());
        assertEquals("PLACED", order.path("status").asText());
        assertMoney("199", order.path("subtotal"));
        assertMoney("40", order.path("deliveryFee"));
        assertMoney("9.95", order.path("tax"));
        assertMoney("5", order.path("platformFee"));
        assertMoney("253.95", order.path("total"));
        assertFalse(order.path("createdAt").asText().isBlank());
        assertTrue(order.path("id").asLong() > 0);
    }

    @Test
    void ordersAtOrAboveTheThresholdDeliverFree() throws Exception {
        var reply = call("POST", "/api/orders", orderBody(id(pizza, "Farmhouse Pizza"), 2), token);
        assertEquals(201, reply.status());
        JsonNode order = reply.json();
        assertMoney("598", order.path("subtotal"));
        assertMoney("0", order.path("deliveryFee"));
        assertMoney("29.90", order.path("tax"));
        assertMoney("632.90", order.path("total"));
        JsonNode line = order.path("items").path(0);
        assertEquals("Farmhouse Pizza", line.path("name").asText());
        assertEquals(2, line.path("quantity").asInt());
        assertMoney("299", line.path("price"));
    }

    @Test
    void duplicateLinesAreMergedAndMultipleItemsAreSummed() throws Exception {
        long margherita = id(pizza, "Margherita Pizza");
        long cola = id(pizza, "Cola");
        var reply = call("POST", "/api/orders", orderBody(margherita, 1, cola, 2, margherita, 2), token);
        assertEquals(201, reply.status());
        JsonNode order = reply.json();
        assertEquals(2, order.path("items").size());
        assertEquals(3, order.path("items").path(0).path("quantity").asInt());
        assertEquals("Margherita Pizza", order.path("items").path(0).path("name").asText());
        assertMoney(String.valueOf(3 * 199 + 2 * 60), order.path("subtotal"));
    }

    @Test
    void clientSuppliedPricesAreIgnored() throws Exception {
        String body = "{\"items\":[{\"menuItemId\":" + id(pizza, "Margherita Pizza")
                + ",\"quantity\":1,\"price\":1,\"total\":1}],\"total\":1}";
        var reply = call("POST", "/api/orders", body, token);
        assertEquals(201, reply.status());
        assertMoney("199", reply.json().path("subtotal"));
        assertMoney("253.95", reply.json().path("total"));
    }

    @Test
    void quantityLimitsApplyPerLineAndAfterMerging() throws Exception {
        long item = id(pizza, "Margherita Pizza");
        assertEquals(201, call("POST", "/api/orders", orderBody(item, 20), token).status());

        var tooMany = call("POST", "/api/orders", orderBody(item, 21), token);
        assertEquals(400, tooMany.status());
        assertTrue(tooMany.message().contains("quantity"), tooMany.body());

        var merged = call("POST", "/api/orders", orderBody(item, 15, item, 10), token);
        assertEquals(400, merged.status());
        assertEquals("Maximum 20 units per item", merged.message());

        assertEquals(400, call("POST", "/api/orders", orderBody(item, 0), token).status());
        assertEquals(400, call("POST", "/api/orders", orderBody(item, -1), token).status());
    }

    @Test
    void invalidRequestShapesAreRejected() throws Exception {
        long item = id(pizza, "Margherita Pizza");
        assertEquals(400, call("POST", "/api/orders", "{\"items\":[]}", token).status());
        assertEquals(400, call("POST", "/api/orders", "{}", token).status());
        assertEquals(400, call("POST", "/api/orders", "{\"items\":[{\"quantity\":1}]}", token).status());
        assertEquals(400, call("POST", "/api/orders", "{bad", token).status());

        long[] fiftyOne = new long[102];
        for (int i = 0; i < 102; i += 2) {
            fiftyOne[i] = item;
            fiftyOne[i + 1] = 1;
        }
        assertEquals(400, call("POST", "/api/orders", orderBody(fiftyOne), token).status());
    }

    @Test
    void unknownMenuItemsAndMixedRestaurantsAreRejected() throws Exception {
        var unknown = call("POST", "/api/orders", orderBody(999_999, 1), token);
        assertEquals(400, unknown.status());
        assertEquals("Some menu items do not exist", unknown.message());

        var mixed = call("POST", "/api/orders",
                orderBody(id(pizza, "Margherita Pizza"), 1, id(burger, "Fries"), 1), token);
        assertEquals(400, mixed.status());
        assertEquals("An order can contain items from one restaurant only", mixed.message());
    }

    @Test
    void historyIsNewestFirstAndPrivateToEachCustomer() throws Exception {
        assertTrue(call("GET", "/api/orders", null, token).json().isEmpty());

        long first = call("POST", "/api/orders", orderBody(id(pizza, "Cola"), 1), token).json().path("id").asLong();
        long second = call("POST", "/api/orders", orderBody(id(burger, "Fries"), 1), token).json().path("id").asLong();

        JsonNode history = call("GET", "/api/orders", null, token).json();
        assertEquals(2, history.size());
        assertEquals(second, history.path(0).path("id").asLong());
        assertEquals(first, history.path(1).path("id").asLong());
        assertEquals("Burger Barn", history.path(0).path("restaurantName").asText());

        String other = newCustomerToken();
        assertTrue(call("GET", "/api/orders", null, other).json().isEmpty());
    }

    @Test
    void ordersRequireAuthentication() throws Exception {
        assertEquals(401, call("GET", "/api/orders", null, null).status());
        assertEquals(401, call("POST", "/api/orders", orderBody(1, 1), null).status());
    }

    @Test
    void ordersCannotBeModifiedOrDeletedThroughTheApi() throws Exception {
        long order = call("POST", "/api/orders", orderBody(id(pizza, "Cola"), 1), token).json().path("id").asLong();
        for (String method : new String[] {"PUT", "PATCH", "DELETE"}) {
            int status = call(method, "/api/orders/" + order, "{}", token).status();
            assertTrue(status == 404 || status == 405, method + " -> " + status);
        }
    }
}
