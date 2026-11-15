package com.tw.joi.delivery.exception;

public class OrderDoesNotExistException extends RuntimeException {

    public OrderDoesNotExistException(String orderId) {
        super("Order: " + orderId + " does not exist");
    }
}