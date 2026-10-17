package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OpenApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void documentDescribesEveryEndpointAndBearerAuthentication() throws Exception {
        var response = request("GET", "/v3/api-docs", null, null);
        assertEquals(200, response.statusCode());
        JsonNode document = mapper.readTree(response.body());
        assertEquals("Rukmini Eat API", document.path("info").path("title").asText());
        String description = document.path("info").path("description").asText();
        for (String route : List.of("/login", "/restaurants", "/restaurants/:id", "/cart", "/orders")) {
            assertTrue(description.contains("`" + route + "`"), route);
        }
        assertTrue(document.path("servers").path(0).path("url").asText().endsWith("/rukmini-eat"));
        assertEquals("bearer", document.at("/components/securitySchemes/bearerAuth/scheme").asText());
        assertTrue(document.path("security").path(0).has("bearerAuth"));
        JsonNode paths = document.path("paths");
        for (String path : List.of("/api/auth/login", "/api/auth/register", "/api/restaurants",
                "/api/restaurants/{id}", "/api/orders", "/api/admin/restaurants",
                "/api/admin/restaurants/{id}/menu-items", "/api/integrations/providers")) {
            assertTrue(paths.has(path), path);
        }
        for (String path : List.of("/api/auth/login", "/api/auth/register")) {
            assertTrue(paths.path(path).path("post").has("security"));
            assertEquals(0, paths.path(path).path("post").path("security").size());
        }
        assertTrue(paths.path("/api/auth/register").path("post").path("responses").has("201"));
        assertTrue(paths.path("/api/orders").path("post").path("responses").has("201"));
        JsonNode parameters = paths.path("/api/restaurants").path("get").path("parameters");
        assertEquals(6, parameters.size());
        for (String name : List.of("q", "cuisine", "vegOnly", "topRated", "fast", "sort")) {
            boolean present = false;
            for (JsonNode parameter : parameters) {
                present |= name.equals(parameter.path("name").asText());
            }
            assertTrue(present, name);
        }
        assertEquals(0, paths.path("/api/orders").path("get").path("parameters").size());
        assertFalse(paths.path("/api/orders").path("post").path("requestBody").isMissingNode());
        assertResponses(paths, "/api/auth/register", "post", "201", "400", "409", "429");
        assertResponses(paths, "/api/auth/login", "post", "200", "400", "401", "429");
        assertResponses(paths, "/api/restaurants", "get", "200", "401");
        assertResponses(paths, "/api/restaurants/{id}", "get", "200", "401", "404");
        assertResponses(paths, "/api/orders", "post", "201", "400", "401");
        assertResponses(paths, "/api/orders", "get", "200", "401");
        assertResponses(paths, "/api/admin/restaurants", "post", "201", "400", "401", "403");
        assertResponses(paths, "/api/admin/restaurants/{id}/menu-items", "post",
                "201", "400", "401", "403", "404");
        assertResponses(paths, "/api/integrations/providers", "get", "200", "401");
        assertEquals("demo@rukmini.com",
                document.at("/components/schemas/LoginRequest/properties/email/example").asText());
        assertTrue(document.at("/components/schemas").has("ApiError"));
    }

    private void assertResponses(JsonNode paths, String path, String method, String... codes) {
        for (String code : codes) {
            assertTrue(paths.path(path).path(method).path("responses").has(code), method + " " + path + " " + code);
        }
    }

    @Test
    void localConsoleAndDocsAliasesBehaveAsDocumented() throws Exception {
        assertEquals(401, request("GET", "/h2-console", null, null).statusCode());
        var alias = request("GET", "/swagger-ui.html", null, null);
        assertTrue(alias.statusCode() == 200 || alias.statusCode() == 302);
    }

    @Test
    void swaggerUiAndItsConfigurationArePublic() throws Exception {
        var page = request("GET", "/swagger-ui/index.html", null, null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("swagger-ui"));
        assertEquals(200, request("GET", "/swagger-ui/swagger-ui-bundle.js", null, null).statusCode());
        var config = request("GET", "/v3/api-docs/swagger-config", null, null);
        assertEquals(200, config.statusCode());
        assertEquals("/rukmini-eat/v3/api-docs", mapper.readTree(config.body()).path("url").asText());
        assertEquals(200, request("GET", "/v3/api-docs.yaml", null, null).statusCode());
    }

    @Test
    void registrationLoginAndOrderingWorkUnderTheContextPath() throws Exception {
        String email = "swagger-" + UUID.randomUUID() + "@example.com";
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"Test-pass-123\"}";
        var registered = request("POST", "/api/auth/register",
                "{\"name\":\"Swagger test\"," + credentials.substring(1), null);
        assertEquals(201, registered.statusCode());
        var login = request("POST", "/api/auth/login", credentials, null);
        assertEquals(200, login.statusCode());
        String token = mapper.readTree(login.body()).path("token").asText();
        assertFalse(token.isBlank());
        assertEquals(409, request("POST", "/api/auth/register",
                "{\"name\":\"Swagger test\"," + credentials.substring(1), null).statusCode());
        var restaurants = request("GET", "/api/restaurants?sort=rating", null, token);
        assertEquals(200, restaurants.statusCode());
        long restaurantId = mapper.readTree(restaurants.body()).path(0).path("id").asLong();
        var detail = request("GET", "/api/restaurants/" + restaurantId, null, token);
        assertEquals(200, detail.statusCode());
        long menuItemId = mapper.readTree(detail.body()).path("menu").path(0).path("id").asLong();
        var placed = request("POST", "/api/orders",
                "{\"items\":[{\"menuItemId\":" + menuItemId + ",\"quantity\":1}]}", token);
        assertEquals(201, placed.statusCode());
        JsonNode order = mapper.readTree(placed.body());
        var history = request("GET", "/api/orders", null, token);
        assertEquals(200, history.statusCode());
        assertEquals(order.path("id").asLong(),         mapper.readTree(history.body()).path(0).path("id").asLong());
                assertEquals(order.path("total"), mapper.readTree(history.body()).path(0).path("total"));
        assertEquals(400, request("POST", "/api/orders", "{\"items\":[]}", token).statusCode());
        assertEquals(403, request("POST", "/api/admin/restaurants", "{}", token).statusCode());
        var providers = request("GET", "/api/integrations/providers", null, token);
        assertEquals(200, providers.statusCode());
        assertFalse(mapper.readTree(providers.body()).path(0).path("liveData").asBoolean());
    }

    @Test
    void documentationDoesNotMakeApplicationEndpointsPublic() throws Exception {
        for (String path : List.of("/api/restaurants", "/api/orders", "/api/integrations/providers")) {
            assertEquals(401, request("GET", path, null, null).statusCode(), path);
            assertEquals(401, request("GET", path, null, "invalid-token").statusCode(), path);
        }
        assertEquals(401, request("POST", "/api/auth/login",
                "{\"email\":\"missing@example.com\",\"password\":\"incorrect\"}", null).statusCode());
    }

    @Test
    void frontendOriginCanPreflightAuthenticatedApiRequests() throws Exception {
        var response = client.send(HttpRequest.newBuilder(uri("/api/restaurants"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("http://localhost:4200",
                response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    private HttpResponse<String> request(String method, String path, String body, String token)
            throws Exception {
        var builder = HttpRequest.newBuilder(uri(path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + "/rukmini-eat" + path);
    }
}
