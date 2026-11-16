package com.tw.joi.delivery.exception;

import com.tw.joi.delivery.domain.OrderStatus;

public class InvalidOrderStatusTransitionException extends IllegalStateException {

    public InvalidOrderStatusTransitionException(OrderStatus current, OrderStatus next) {
        super("Cannot transition order from " + current + " to " + next);
    }
}
