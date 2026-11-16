package com.tw.joi.delivery.domain;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Outlet<P extends Product> {

    protected String name;

    protected String description;

    protected String outletId;

    public abstract void addProduct(P product);

    public abstract Optional<P> findProduct(String productId);

    protected final void validateProductForRegistration(P product) {
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }
        if (outletId == null || outletId.isBlank()) {
            throw new IllegalArgumentException("Outlet ID must not be blank");
        }
        if (product.getProductId() == null || product.getProductId().isBlank()) {
            throw new IllegalArgumentException("Product ID must not be blank");
        }
        validateOwner(product.getOutlet());
    }

    protected final void validateOwner(Outlet<?> existingOwner) {
        if (existingOwner != null && !outletId.equals(existingOwner.getOutletId())) {
            throw new IllegalArgumentException("Product already belongs to another outlet");
        }
    }
}
