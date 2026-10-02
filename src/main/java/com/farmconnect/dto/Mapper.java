package com.farmconnect.dto;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.model.*;

public final class Mapper {
    private Mapper() {}

    public static FarmResponse farm(Farm f) {
        return new FarmResponse(f.getId(), f.getName(), f.getLocation(), f.getDescription(), f.getOwner().getId());
    }

    public static ProductResponse product(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getPrice(),
                p.getQuantity(), p.getUnit(), p.getImageUrl(), p.getFarm().getId(), p.getFarm().getName());
    }

    public static CartItemResponse cartItem(CartItem c) {
        return new CartItemResponse(c.getId(), product(c.getProduct()), c.getQuantity());
    }

    public static OrderResponse order(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotal(), o.getDeliveryAddress(), o.getCreatedAt(),
                o.getItems().stream()
                        .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(), i.getPrice()))
                        .toList());
    }
}
