package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.RestaurantDetail;
import com.rukamini.eat.dto.Dto.RestaurantSummary;
import com.rukamini.eat.service.RestaurantService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurants;

    public RestaurantController(RestaurantService restaurants) {
        this.restaurants = restaurants;
    }

    @GetMapping
    public List<RestaurantSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cuisine,
            @RequestParam(defaultValue = "false") boolean vegOnly,
            @RequestParam(defaultValue = "false") boolean topRated,
            @RequestParam(defaultValue = "false") boolean fast,
            @RequestParam(required = false) String sort) {
        return restaurants.search(q, cuisine, vegOnly, topRated, fast, sort);
    }

    @GetMapping("/{id}")
    public RestaurantDetail get(@PathVariable Long id) {
        return restaurants.get(id);
    }
}
