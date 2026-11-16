package com.tw.joi.delivery.exception;

import java.math.BigDecimal;

public class PriceChangedException extends IllegalStateException {

    public PriceChangedException(String productId, BigDecimal cartPrice, BigDecimal currentPrice) {
        super("Order not placed: price for product " + productId
                + " changed from " + cartPrice.toPlainString()
                + " to " + currentPrice.toPlainString() + ".");
    }
}
