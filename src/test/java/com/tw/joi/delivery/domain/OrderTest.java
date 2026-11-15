package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private final Instant createdAt =
            Instant.parse("2026-11-12T10:00:00Z");

    @Test
    void shouldCreatePlacedOrderAndCalculateTotal() {
        Order order = new Order(
                "order101", "user101", "store101",
                List.of(
                        item("bread", 2, "10.50"),
                        item("milk", 1, "5.00")
                ),
                createdAt
        );

        assertEquals("order101", order.getOrderId());
        assertEquals("user101", order.getUserId());
        assertEquals("store101", order.getOutletId());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(createdAt, order.getCreatedAt());
        assertEquals(new BigDecimal("26.00"), order.getTotalPrice());
    }

    @Test
    void shouldPreserveItemsWhenOriginalListChanges() {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(item("bread", 2, "10.50"));

        Order order = new Order(
                "order101", "user101", "store101",
                cartItems, createdAt
        );

        cartItems.clear();

        assertEquals(1, order.getItems().size());
        assertEquals(new BigDecimal("21.00"), order.getTotalPrice());
        assertThrows(
                UnsupportedOperationException.class,
                () -> order.getItems().clear()
        );
    }

    @Test
    void shouldRejectEmptyOrder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Order(
                        "order101", "user101", "store101",
                        List.of(), createdAt
                )
        );
    }

    @Test
    void shouldProgressThroughDeliveryLifecycle() {
        Order order = newOrder();

        OrderStatus[] path = {
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
        };

        for (OrderStatus next : path) {
            order.transitionTo(next);

            assertEquals(next, order.getStatus());
        }
    }

    @Test
    void shouldRejectSkippedStatusWithoutChangingState() {
        Order order = newOrder();

        assertThrows(
                IllegalStateException.class,
                () -> order.transitionTo(OrderStatus.DELIVERED)
        );

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    void shouldRejectCancellationAfterPreparationStarts() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.PREPARING);

        assertThrows(
                IllegalStateException.class,
                () -> order.transitionTo(OrderStatus.CANCELLED)
        );

        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }

    @Test
    void shouldRejectChangesAfterCancellation() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CANCELLED);

        assertThrows(
                IllegalStateException.class,
                () -> order.transitionTo(OrderStatus.CONFIRMED)
        );

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldRejectNullStatusWithoutChangingState() {
        Order order = newOrder();

        assertThrows(
                NullPointerException.class,
                () -> order.transitionTo(null)
        );

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    private Order newOrder() {
        return new Order(
                "order101",
                "user101",
                "store101",
                List.of(item("bread", 2, "10.50")),
                createdAt
        );
    }

    private CartItem item(String productId, int quantity, String price) {
        GroceryProduct product = GroceryProduct.builder()
                .productId(productId)
                .productName(productId)
                .build();

        return new CartItem(product, quantity, new BigDecimal(price));
    }
}