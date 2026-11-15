package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public final class Order {

    private final String orderId;
    private final String userId;
    private final String outletId;
    private final List<CartItem> items;
    private final Instant createdAt;
    private OrderStatus status;

    public Order(
            String orderId,
            String userId,
            String outletId,
            List<CartItem> items,
            Instant createdAt
    ) {
        Assert.hasText(orderId, "Order ID must not be blank");
        this.orderId = orderId;

        Assert.hasText(userId, "User ID must not be blank");
        this.userId = userId;

        Assert.hasText(outletId, "Outlet ID must not be blank");
        this.outletId = outletId;

        Objects.requireNonNull(items, "Items must not be null");

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }

        this.items = List.copyOf(items);
        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation time must not be null"
        );
        this.status = OrderStatus.PLACED;
    }

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void transitionTo(OrderStatus next) {
        Objects.requireNonNull(next, "Next status must not be null");

        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Cannot transition order from " + status + " to " + next
            );
        }

        this.status = next;
    }
}