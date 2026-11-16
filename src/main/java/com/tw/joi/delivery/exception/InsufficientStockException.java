package com.tw.joi.delivery.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, long requestedQuantity, int availableStock) {
        super("Requested quantity " + requestedQuantity
                + " exceeds available stock " + availableStock
                + " for product " + productId);
    }
}
