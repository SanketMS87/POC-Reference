package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.RestaurantDetail;
import com.rukamini.eat.dto.Dto.RestaurantSummary;
import com.rukamini.eat.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@Tag(name = "Restaurants")
public class RestaurantController {

    private final RestaurantService restaurants;

    public RestaurantController(RestaurantService restaurants) {
        this.restaurants = restaurants;
    }

    @GetMapping
    @Operation(summary = "Search and filter restaurants")
    @ApiResponse(responseCode = "200", description = "Matching restaurants; empty list when none match")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    public List<RestaurantSummary> list(
            @Parameter(description = "Search restaurant names, cuisines, or menu item names")
            @RequestParam(required = false) String q,
            @Parameter(description = "Cuisine substring, case-insensitive")
            @RequestParam(required = false) String cuisine,
            @RequestParam(defaultValue = "false") boolean vegOnly,
            @Parameter(description = "Only restaurants rated at least 4.3")
            @RequestParam(defaultValue = "false") boolean topRated,
            @Parameter(description = "Only restaurants with an ETA of 25 minutes or less")
            @RequestParam(defaultValue = "false") boolean fast,
            @Parameter(schema = @Schema(allowableValues = {
                    "relevance", "rating", "eta", "costLow", "costHigh"}, defaultValue = "relevance"))
            @RequestParam(required = false) String sort) {
        return restaurants.search(q, cuisine, vegOnly, topRated, fast, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a restaurant and its menu")
    @ApiResponse(responseCode = "200", description = "Restaurant with menu items")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    @ApiResponse(responseCode = "404", description = "Restaurant not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public RestaurantDetail get(@PathVariable Long id) {
        return restaurants.get(id);
    }
}
