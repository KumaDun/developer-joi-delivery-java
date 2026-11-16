package com.tw.joi.delivery.exception;

public class EmptyCartException extends IllegalStateException {

    public EmptyCartException() {
        super("Cannot place an order without a nonempty cart");
    }
}
