package com.tw.joi.delivery.domain;

public enum OrderStatus {
    PLACED,
    CANCELLED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED;

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) {
            return false;
        }

        return switch (this) {
            case PLACED ->
                    next == CONFIRMED || next == CANCELLED;

            case CONFIRMED ->
                    next == PREPARING || next == CANCELLED;

            case PREPARING ->
                    next == READY_FOR_PICKUP;

            case READY_FOR_PICKUP ->
                    next == OUT_FOR_DELIVERY;

            case OUT_FOR_DELIVERY ->
                    next == DELIVERED;

            case DELIVERED, CANCELLED -> false;
        };
    }
}
