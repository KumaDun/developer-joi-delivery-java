package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class FoodProduct extends Product {

    @Builder
    public FoodProduct(String productId, String productName, BigDecimal mrp,
                       BigDecimal unitPrice, int availableStock, Restaurant restaurant) {
        super(productId, productName, mrp, unitPrice, availableStock, restaurant);
    }
}
