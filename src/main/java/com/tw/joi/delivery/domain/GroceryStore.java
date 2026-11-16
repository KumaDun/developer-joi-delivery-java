package com.tw.joi.delivery.domain;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroceryStore extends Outlet<GroceryProduct> {

    private Set<GroceryProduct> inventory=new HashSet<>();

    @Builder
    public GroceryStore(String name, String description, String outletId) {
        super(name, description, outletId);
        this.inventory = new HashSet<>();
    }

    @Override
    public Optional<GroceryProduct> findProduct(String productId) {
        return inventory.stream()
                .filter(product -> productId.equals(product.getProductId()))
                .findFirst();
    }

    @Override
    public void addProduct(GroceryProduct product) {
        if (findProduct(product.getProductId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Product already exists in inventory: " + product.getProductId()
            );
        }

        inventory.add(product);
        product.setStore(this);
        product.setOutlet(this);
    }
}
