package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.MenuItemCreateRequest;
import com.rukamini.eat.dto.Dto.MenuItemDto;
import com.rukamini.eat.dto.Dto.RestaurantCreateRequest;
import com.rukamini.eat.dto.Dto.RestaurantSummary;
import com.rukamini.eat.service.RestaurantService;
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
public class AdminController {

    private final RestaurantService restaurants;

    public AdminController(RestaurantService restaurants) {
        this.restaurants = restaurants;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantSummary create(@Valid @RequestBody RestaurantCreateRequest request) {
        return restaurants.create(request);
    }

    @PostMapping("/{id}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemDto addMenuItem(@PathVariable Long id, @Valid @RequestBody MenuItemCreateRequest request) {
        return restaurants.addMenuItem(id, request);
    }
}
