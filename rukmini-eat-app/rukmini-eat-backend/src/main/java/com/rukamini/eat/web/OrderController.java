package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.OrderDto;
import com.rukamini.eat.dto.Dto.OrderRequest;
import com.rukamini.eat.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderRequest request) {
        return orders.place(jwt.getSubject(), request);
    }

    @GetMapping
    public List<OrderDto> history(@AuthenticationPrincipal Jwt jwt) {
        return orders.history(jwt.getSubject());
    }
}
