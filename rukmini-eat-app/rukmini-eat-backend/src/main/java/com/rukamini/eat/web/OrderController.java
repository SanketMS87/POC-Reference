package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.OrderDto;
import com.rukamini.eat.dto.Dto.OrderRequest;
import com.rukamini.eat.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @Operation(summary = "Place a local order",
            description = "Items must belong to one restaurant. Maximum 20 units per item and "
                    + "50 request lines. Prices and fees are recalculated on the server; "
                    + "orders are not sent to external providers.")
    @ApiResponse(responseCode = "201", description = "Order placed with server-calculated totals")
    @ApiResponse(responseCode = "400",
            description = "Invalid request, unknown menu item, quantity over 20, or items from multiple restaurants",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                          @Valid @RequestBody OrderRequest request) {
        return orders.place(jwt.getSubject(), request);
    }

    @GetMapping
    @Operation(summary = "List the signed-in customer's orders, newest first")
    @ApiResponse(responseCode = "200", description = "Orders for the signed-in customer; empty list when none")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    public List<OrderDto> history(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return orders.history(jwt.getSubject());
    }
}
