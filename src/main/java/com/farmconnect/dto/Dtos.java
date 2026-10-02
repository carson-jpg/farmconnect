package com.farmconnect.dto;

import com.farmconnect.model.OrderStatus;
import com.farmconnect.model.Role;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email, String phone,
                                  @NotBlank @Size(min = 6) String password, @NotNull Role role) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, Long id, String name, String email, Role role) {}

    public record FarmRequest(@NotBlank String name, String location, String description) {}
    public record FarmResponse(Long id, String name, String location, String description, Long ownerId) {}

    public record ProductRequest(@NotBlank String name, String description, String category,
                                 @NotNull @DecimalMin("0.0") BigDecimal price, @Min(0) int quantity,
                                 String unit, String imageUrl, @NotNull Long farmId) {}
    public record ProductResponse(Long id, String name, String description, String category, BigDecimal price,
                                  int quantity, String unit, String imageUrl, Long farmId, String farmName) {}

    public record CartRequest(@NotNull Long productId, @Min(1) int quantity) {}
    public record QuantityRequest(@Min(1) int quantity) {}
    public record CartItemResponse(Long id, ProductResponse product, int quantity) {}

    public record OrderRequest(@NotBlank String deliveryAddress) {}
    public record StatusRequest(@NotNull OrderStatus status) {}
    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal price) {}
    public record OrderResponse(Long id, OrderStatus status, BigDecimal total, String deliveryAddress,
                                Instant createdAt, List<OrderItemResponse> items) {}
}
