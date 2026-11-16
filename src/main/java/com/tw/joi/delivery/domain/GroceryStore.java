package com.tw.joi.delivery.domain;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class GroceryStore extends Outlet<GroceryProduct> {

    private final Set<GroceryProduct> inventory = new HashSet<>();

    @Builder
    public GroceryStore(String name, String description, String outletId) {
        super(name, description, outletId);
    }

    public synchronized Set<GroceryProduct> getInventory() {
        return Set.copyOf(inventory);
    }

    @Override
    public synchronized Optional<GroceryProduct> findProduct(String productId) {
        return inventory.stream()
                .filter(product -> productId.equals(product.getProductId()))
                .findFirst();
    }

    @Override
    public synchronized void addProduct(GroceryProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }

        synchronized (product) {
            validateProductForRegistration(product);
            validateOwner(product.getStore());
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
}
