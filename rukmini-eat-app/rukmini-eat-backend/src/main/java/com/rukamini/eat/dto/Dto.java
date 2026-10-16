package com.rukamini.eat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dto {

    private Dto() {
    }

    // ---- auth ----
    public record RegisterRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 6, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserDto(Long id, String name, String email, String role) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    // ---- restaurants ----
    public record MenuItemDto(Long id, String name, String category, BigDecimal price,
                              boolean veg, boolean bestseller, String emoji) {
    }

    public record RestaurantSummary(Long id, String name, List<String> cuisines, double rating,
                                    int etaMinutes, int costForTwo, boolean pureVeg,
                                    String emoji, String description) {
    }

    public record RestaurantDetail(Long id, String name, List<String> cuisines, double rating,
                                   int etaMinutes, int costForTwo, boolean pureVeg,
                                   String emoji, String description, List<MenuItemDto> menu) {
    }

    // ---- orders ----
    public record OrderLineRequest(@NotNull Long menuItemId, @Min(1) @Max(20) int quantity) {
    }

    public record OrderRequest(@NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> items) {
    }

    public record OrderLineDto(String name, BigDecimal price, int quantity) {
    }

    public record OrderDto(Long id, String restaurantName, String status, List<OrderLineDto> items,
                           BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal tax,
                           BigDecimal platformFee, BigDecimal total, Instant createdAt) {
    }

    // ---- admin ----
    public record RestaurantCreateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 200) String cuisines,
            @NotNull @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
            @Min(5) @Max(180) int etaMinutes,
            @Min(0) int costForTwo,
            boolean pureVeg,
            @Size(max = 8) String emoji,
            @Size(max = 300) String description) {
    }

    public record MenuItemCreateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 60) String category,
            @NotNull @DecimalMin("1.0") BigDecimal price,
            boolean veg,
            boolean bestseller,
            @Size(max = 8) String emoji) {
    }
}
