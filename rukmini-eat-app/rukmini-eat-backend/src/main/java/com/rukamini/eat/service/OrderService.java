package com.rukamini.eat.service;

import com.rukamini.eat.domain.MenuItem;
import com.rukamini.eat.domain.Order;
import com.rukamini.eat.domain.OrderItem;
import com.rukamini.eat.domain.Restaurant;
import com.rukamini.eat.domain.User;
import com.rukamini.eat.dto.Dto.OrderDto;
import com.rukamini.eat.dto.Dto.OrderLineRequest;
import com.rukamini.eat.dto.Dto.OrderRequest;
import com.rukamini.eat.repository.MenuItemRepository;
import com.rukamini.eat.repository.OrderRepository;
import com.rukamini.eat.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final int MAX_QTY_PER_ITEM = 20;

    private final OrderRepository orders;
    private final MenuItemRepository menuItems;
    private final UserRepository users;

    public OrderService(OrderRepository orders, MenuItemRepository menuItems, UserRepository users) {
        this.orders = orders;
        this.menuItems = menuItems;
        this.users = users;
    }

    /** Prices are always taken from the database, never from the client. */
    @Transactional
    public OrderDto place(String email, OrderRequest req) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest line : req.items()) {
            quantities.merge(line.menuItemId(), line.quantity(), (a, b) -> a + b);
        }
        if (quantities.values().stream().anyMatch(q -> q > MAX_QTY_PER_ITEM)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum " + MAX_QTY_PER_ITEM + " units per item");
        }

        Map<Long, MenuItem> found = new HashMap<>();
        for (MenuItem item : menuItems.findAllById(quantities.keySet())) {
            found.put(item.getId(), item);
        }
        if (found.size() != quantities.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Some menu items do not exist");
        }

        Restaurant restaurant = found.values().iterator().next().getRestaurant();
        boolean singleRestaurant = found.values().stream()
                .allMatch(m -> m.getRestaurant().getId().equals(restaurant.getId()));
        if (!singleRestaurant) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An order can contain items from one restaurant only");
        }

        Order order = new Order(user, restaurant);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            MenuItem item = found.get(entry.getKey());
            order.addItem(new OrderItem(item.getName(), item.getPrice(), entry.getValue()));
            subtotal = subtotal.add(item.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        Pricing.Totals t = Pricing.of(subtotal);
        order.applyTotals(t.subtotal(), t.deliveryFee(), t.tax(), t.platformFee(), t.total());

        return Mapper.order(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> history(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return orders.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(o -> Mapper.order(o)).toList();
    }
}
