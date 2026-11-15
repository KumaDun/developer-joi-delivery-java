package com.tw.joi.delivery.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Restaurant extends Outlet<FoodProduct> {

    private final List<FoodProduct> menu = new ArrayList<>();

    @Override
    public Optional<FoodProduct> findProduct(String productId) {
        return menu.stream()
                .filter(product -> product.getProductId().equals(productId))
                .findFirst();
    }

    public void addFoodProduct(FoodProduct product) {
        if (findProduct(product.getProductId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Product already exists in menu: " + product.getProductId()
            );
        }

        menu.add(product);
    }
}