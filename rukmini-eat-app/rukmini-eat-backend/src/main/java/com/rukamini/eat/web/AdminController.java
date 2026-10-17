package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.MenuItemCreateRequest;
import com.rukamini.eat.dto.Dto.MenuItemDto;
import com.rukamini.eat.dto.Dto.RestaurantCreateRequest;
import com.rukamini.eat.dto.Dto.RestaurantSummary;
import com.rukamini.eat.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Restricted to the ADMIN role in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/restaurants")
@Tag(name = "Administration", description = "Requires a JWT with the ADMIN role.")
public class AdminController {

    private final RestaurantService restaurants;

    public AdminController(RestaurantService restaurants) {
        this.restaurants = restaurants;
    }

    @PostMapping
    @Operation(summary = "Create a restaurant (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Restaurant created")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Signed-in user is not an ADMIN", content = @Content)
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantSummary create(@Valid @RequestBody RestaurantCreateRequest request) {
        return restaurants.create(request);
    }

    @PostMapping("/{id}/menu-items")
    @Operation(summary = "Add a menu item to a restaurant (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Menu item added")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Signed-in user is not an ADMIN", content = @Content)
    @ApiResponse(responseCode = "404", description = "Restaurant not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemDto addMenuItem(@PathVariable Long id, @Valid @RequestBody MenuItemCreateRequest request) {
        return restaurants.addMenuItem(id, request);
    }
}
