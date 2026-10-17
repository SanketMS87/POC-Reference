package com.rukamini.eat.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared HTTP helpers. Subclasses use one cached application context with an in-memory database,
 * so tests create uniquely named data and never assume absolute row counts.
 */
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:api-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.rate-limit.max-attempts=0"
})
abstract class ApiTestSupport {

    static final String PASSWORD = "Test-pass-123";

    record Reply(int status, String body, ObjectMapper mapper) {
        JsonNode json() {
            return mapper.readTree(body);
        }

        String message() {
            return json().path("message").asText();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    protected ObjectMapper mapper;

    private final HttpClient client = HttpClient.newHttpClient();

    protected Reply call(String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rukmini-eat" + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body(), mapper);
    }

    protected HttpResponse<String> raw(HttpRequest.Builder builder) throws Exception {
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected URI uri(String path) {
        return URI.create("http://localhost:" + port + "/rukmini-eat" + path);
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected String registerBody(String name, String email, String password) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    protected String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    protected String newCustomerToken() throws Exception {
        Reply reply = call("POST", "/api/auth/register", registerBody("Test user", uniqueEmail(), PASSWORD), null);
        if (reply.status() != 201) {
            throw new IllegalStateException("Registration failed: " + reply.status() + " " + reply.body());
        }
        return reply.json().path("token").asText();
    }

    protected String loginToken(String email, String password) throws Exception {
        Reply reply = call("POST", "/api/auth/login", loginBody(email, password), null);
        if (reply.status() != 200) {
            throw new IllegalStateException("Login failed: " + reply.status() + " " + reply.body());
        }
        return reply.json().path("token").asText();
    }

    protected String adminToken() throws Exception {
        return loginToken("admin@rukmini.com", "Admin@123");
    }

    protected JsonNode restaurants(String token, String query) throws Exception {
        Reply reply = call("GET", "/api/restaurants" + query, null, token);
        if (reply.status() != 200) {
            throw new IllegalStateException("Restaurant list failed: " + reply.status());
        }
        return reply.json();
    }

    protected JsonNode restaurantNamed(String token, String name) throws Exception {
        for (JsonNode restaurant : restaurants(token, "")) {
            if (name.equals(restaurant.path("name").asText())) {
                return restaurant;
            }
        }
        throw new IllegalStateException("Restaurant not seeded: " + name);
    }

    protected JsonNode detailOf(String token, JsonNode restaurant) throws Exception {
        return call("GET", "/api/restaurants/" + restaurant.path("id").asLong(), null, token).json();
    }

    protected JsonNode menuItemNamed(JsonNode detail, String name) {
        for (JsonNode item : detail.path("menu")) {
            if (name.equals(item.path("name").asText())) {
                return item;
            }
        }
        throw new IllegalStateException("Menu item not found: " + name);
    }

    protected String orderBody(long... idAndQuantityPairs) {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < idAndQuantityPairs.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"menuItemId\":").append(idAndQuantityPairs[i])
                    .append(",\"quantity\":").append(idAndQuantityPairs[i + 1]).append('}');
        }
        return sb.append("]}").toString();
    }
}
