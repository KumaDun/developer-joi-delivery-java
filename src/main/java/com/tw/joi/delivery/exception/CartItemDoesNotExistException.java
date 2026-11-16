package com.tw.joi.delivery.exception;

public class CartItemDoesNotExistException extends RuntimeException {

    public CartItemDoesNotExistException(String cartItemId) {
        super("Cart item: " + cartItemId + " does not exist");
    }
}
