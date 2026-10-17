package com.rukamini.eat.service;

import com.rukamini.eat.domain.MenuItem;
import com.rukamini.eat.domain.Order;
import com.rukamini.eat.domain.Restaurant;
import com.rukamini.eat.domain.User;
import com.rukamini.eat.dto.Dto.MenuItemDto;
import com.rukamini.eat.dto.Dto.OrderDto;
import com.rukamini.eat.dto.Dto.OrderLineDto;
import com.rukamini.eat.dto.Dto.RestaurantDetail;
import com.rukamini.eat.dto.Dto.RestaurantSummary;
import com.rukamini.eat.dto.Dto.UserDto;
import java.util.Arrays;
import java.util.List;

public final class Mapper {

    private Mapper() {
    }

    public static UserDto user(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }

    public static List<String> cuisines(Restaurant r) {
        return Arrays.stream(r.getCuisines().split(","))
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static RestaurantSummary summary(Restaurant r) {
        return new RestaurantSummary(r.getId(), r.getName(), cuisines(r), r.getRating(),
                r.getEtaMinutes(), r.getCostForTwo(), r.isPureVeg(), r.getEmoji(), r.getDescription());
    }

    public static RestaurantDetail detail(Restaurant r) {
        return new RestaurantDetail(r.getId(), r.getName(), cuisines(r), r.getRating(),
                r.getEtaMinutes(), r.getCostForTwo(), r.isPureVeg(), r.getEmoji(), r.getDescription(),
                r.getMenu().stream().map(m -> menuItem(m)).toList());
    }

    public static MenuItemDto menuItem(MenuItem m) {
        return new MenuItemDto(m.getId(), m.getName(), m.getCategory(), m.getPrice(),
                m.isVeg(), m.isBestseller(), m.getEmoji());
    }

    public static OrderDto order(Order o) {
        List<OrderLineDto> lines = o.getItems().stream()
                .map(i -> new OrderLineDto(i.getName(), i.getUnitPrice(), i.getQuantity()))
                .toList();
        return new OrderDto(o.getId(), o.getRestaurant().getName(), o.getStatus().name(), lines,
                o.getSubtotal(), o.getDeliveryFee(), o.getTax(), o.getPlatformFee(), o.getTotal(),
                o.getCreatedAt());
    }
}
