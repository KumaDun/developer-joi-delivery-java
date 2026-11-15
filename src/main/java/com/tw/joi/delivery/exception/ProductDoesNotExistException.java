package com.tw.joi.delivery.exception;

public class ProductDoesNotExistException extends RuntimeException {

    public ProductDoesNotExistException(String productId) {
        super("Product: " + productId + " does not exist");
    }

    public ProductDoesNotExistException(String productId, String outletId) {
        super("Product: " + productId + " does not exist in store: " + outletId);
    }
}