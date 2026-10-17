package com.rukamini.eat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
            @Schema(example = "Asha Rao") @NotBlank @Size(max = 80) String name,
            @Schema(example = "asha@example.com") @NotBlank @Email @Size(max = 160) String email,
            @Schema(example = "Secret@123") @NotBlank @Size(min = 6, max = 72) String password) {
    }

    public record LoginRequest(
            @Schema(example = "demo@rukmini.com") @NotBlank String email,
            @Schema(example = "Demo@123") @NotBlank String password) {
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
    public record OrderLineRequest(@Schema(example = "1") @NotNull Long menuItemId,
                                   @Schema(example = "2") @Min(1) @Max(20) int quantity) {
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
            @Schema(example = "Masala House") @NotBlank @Size(max = 120) String name,
            @Schema(example = "North Indian, Tandoor", description = "Comma-separated cuisines")
            @NotBlank @Size(max = 200) String cuisines,
            @Schema(example = "4.5") @NotNull @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
            @Schema(example = "30") @Min(5) @Max(180) int etaMinutes,
            @Schema(example = "500") @Min(0) int costForTwo,
            boolean pureVeg,
            @Size(max = 8) String emoji,
            @Size(max = 300) String description) {
    }

    public record MenuItemCreateRequest(
            @Schema(example = "Paneer Tikka") @NotBlank @Size(max = 120) String name,
            @Schema(example = "Starters") @NotBlank @Size(max = 60) String category,
            @Schema(example = "249.00") @NotNull @DecimalMin("1.0") BigDecimal price,
            boolean veg,
            boolean bestseller,
            @Size(max = 8) String emoji) {
    }
}
