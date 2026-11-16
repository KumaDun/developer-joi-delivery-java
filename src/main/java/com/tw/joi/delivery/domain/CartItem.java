package com.tw.joi.delivery.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
public class CartItem {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final SpiceLevel spiceLevel;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Boolean utensilsNeeded;

    public CartItem(Product product, int quantity, BigDecimal unitPrice) {
        this(product, quantity, unitPrice, null, null);
    }

    public CartItem(Product product, int quantity, BigDecimal unitPrice,
                    SpiceLevel spiceLevel, Boolean utensilsNeeded) {
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

        if (product instanceof FoodProduct) {
            this.spiceLevel = spiceLevel != null ? spiceLevel : SpiceLevel.NONE;
            this.utensilsNeeded = Boolean.TRUE.equals(utensilsNeeded);
        } else {
            if (spiceLevel != null || utensilsNeeded != null) {
                throw new IllegalArgumentException(
                        "Spice level and utensils are only available for restaurant meals"
                );
            }
            this.spiceLevel = null;
            this.utensilsNeeded = null;
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
        this(product, quantity, Objects.requireNonNull(product, "Product must not be null").getUnitPrice());
    }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}