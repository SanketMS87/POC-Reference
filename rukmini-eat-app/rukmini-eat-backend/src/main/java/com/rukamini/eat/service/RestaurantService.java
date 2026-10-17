package com.rukamini.eat.service;

import com.rukamini.eat.domain.MenuItem;
import com.rukamini.eat.domain.Restaurant;
import com.rukamini.eat.dto.Dto.MenuItemCreateRequest;
import com.rukamini.eat.dto.Dto.MenuItemDto;
import com.rukamini.eat.dto.Dto.RestaurantCreateRequest;
import com.rukamini.eat.dto.Dto.RestaurantDetail;
import com.rukamini.eat.dto.Dto.RestaurantSummary;
import com.rukamini.eat.repository.MenuItemRepository;
import com.rukamini.eat.repository.RestaurantRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private static final int FAST_ETA = 25;
    private static final double TOP_RATING = 4.3;

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;

    public RestaurantService(RestaurantRepository restaurants, MenuItemRepository menuItems) {
        this.restaurants = restaurants;
        this.menuItems = menuItems;
    }

    @Transactional(readOnly = true)
    public List<RestaurantSummary> search(String q, String cuisine, boolean vegOnly,
                                          boolean topRated, boolean fast, String sort) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String cuisineKey = cuisine == null ? "" : cuisine.trim().toLowerCase(Locale.ROOT);

        return restaurants.findAll().stream()
                .filter(r -> !vegOnly || r.isPureVeg())
                .filter(r -> !topRated || r.getRating() >= TOP_RATING)
                .filter(r -> !fast || r.getEtaMinutes() <= FAST_ETA)
                .filter(r -> cuisineKey.isEmpty() || r.getCuisines().toLowerCase(Locale.ROOT).contains(cuisineKey))
                .filter(r -> needle.isEmpty() || matches(r, needle))
                .sorted(comparator(sort))
                .map(r -> Mapper.summary(r))
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantDetail get(Long id) {
        return Mapper.detail(restaurants.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found")));
    }

    @Transactional
    public RestaurantSummary create(RestaurantCreateRequest req) {
        Restaurant r = new Restaurant(req.name().trim(), req.cuisines().trim(), req.rating().doubleValue(),
                req.etaMinutes(), req.costForTwo(), req.pureVeg(),
                req.emoji() == null || req.emoji().isBlank() ? "🍽️" : req.emoji(), req.description());
        return Mapper.summary(restaurants.save(r));
    }

    @Transactional
    public MenuItemDto addMenuItem(Long restaurantId, MenuItemCreateRequest req) {
        Restaurant r = restaurants.findById(restaurantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        MenuItem item = new MenuItem(req.name().trim(), req.category().trim(), req.price(), req.veg(),
                req.bestseller(), req.emoji() == null || req.emoji().isBlank() ? "🍴" : req.emoji());
        r.addMenuItem(item);
        return Mapper.menuItem(menuItems.save(item));
    }

    private boolean matches(Restaurant r, String needle) {
        if (r.getName().toLowerCase(Locale.ROOT).contains(needle)
                || r.getCuisines().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return r.getMenu().stream().anyMatch(m -> m.getName().toLowerCase(Locale.ROOT).contains(needle));
    }

    private Comparator<Restaurant> comparator(String sort) {
        String key = sort == null ? "relevance" : sort;
        return switch (key) {
            case "rating" -> Comparator.comparingDouble((Restaurant r) -> r.getRating()).reversed();
            case "eta" -> Comparator.comparingInt((Restaurant r) -> r.getEtaMinutes());
            case "costLow" -> Comparator.comparingInt((Restaurant r) -> r.getCostForTwo());
            case "costHigh" -> Comparator.comparingInt((Restaurant r) -> r.getCostForTwo()).reversed();
            default -> Comparator.comparingDouble((Restaurant r) -> -(r.getRating() * 2 - r.getEtaMinutes() / 15.0));
        };
    }
}
