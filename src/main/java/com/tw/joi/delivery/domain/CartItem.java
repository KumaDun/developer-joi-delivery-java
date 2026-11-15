package com.tw.joi.delivery.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Setter
@Getter
public class CartItem {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;

    public CartItem(Product product, int quantity, BigDecimal unitPrice) {
        Objects.requireNonNull(product, "Product must not be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        Objects.requireNonNull(unitPrice, "Unit price must not be null");

        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Unit price must not be negative"
            );
        }
        this.productId = Objects.requireNonNull(
                product.getProductId(), "Product ID must not be null"
        );
        this.productName = Objects.requireNonNull(
                product.getProductName(), "Product name must not be null"
        );
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public CartItem(Product product, int quantity) {
        this(product, quantity, product.getUnitPrice());
    }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}