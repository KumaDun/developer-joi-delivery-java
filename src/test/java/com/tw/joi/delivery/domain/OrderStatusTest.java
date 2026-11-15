package com.tw.joi.delivery.domain;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusTest {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED =
            Map.of(
                    OrderStatus.PLACED,
                    Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),

                    OrderStatus.CONFIRMED,
                    Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),

                    OrderStatus.PREPARING,
                    Set.of(OrderStatus.READY_FOR_PICKUP),

                    OrderStatus.READY_FOR_PICKUP,
                    Set.of(OrderStatus.OUT_FOR_DELIVERY),

                    OrderStatus.OUT_FOR_DELIVERY,
                    Set.of(OrderStatus.DELIVERED),

                    OrderStatus.DELIVERED, Set.of(),
                    OrderStatus.CANCELLED, Set.of()
            );

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void shouldAllowOnlyDefinedTransitions(OrderStatus current) {
        for (OrderStatus next : OrderStatus.values()) {
            boolean expected = ALLOWED.get(current).contains(next);

            assertEquals(
                    expected,
                    current.canTransitionTo(next),
                    current + " -> " + next
            );
        }
    }
}