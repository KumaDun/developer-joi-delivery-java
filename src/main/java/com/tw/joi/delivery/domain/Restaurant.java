package com.tw.joi.delivery.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Restaurant extends Outlet<FoodProduct> {

    private final List<FoodProduct> menu = new ArrayList<>();

    @Builder
    public Restaurant(String name, String description, String outletId) {
        super(name, description, outletId);
    }

    public List<FoodProduct> getMenu() {
        return List.copyOf(menu);
    }

    @Override
    public Optional<FoodProduct> findProduct(String productId) {
        return menu.stream()
                .filter(product -> product.getProductId().equals(productId))
                .findFirst();
    }

    @Override
    public void addProduct(FoodProduct product) {
        Objects.requireNonNull(product, "Product must not be null");
        Objects.requireNonNull(product.getProductId(), "Product ID must not be null");

        if (findProduct(product.getProductId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Product already exists in menu: " + product.getProductId()
            );
        }

        menu.add(product);
        product.setOutlet(this);
    }
}
