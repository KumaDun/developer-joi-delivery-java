package com.tw.joi.delivery.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoreTest {

    @Test
    void shouldFindAddedProductEvenWhenStockIsZero() {
        GroceryStore store = GroceryStore.builder()
                .outletId("store-a")
                .build();

        GroceryProduct product = GroceryProduct.builder()
                .productId("product-a")
                .availableStock(0)
                .build();

        store.addProduct(product);

        assertSame(
                product,
                store.findProduct("product-a").orElseThrow()
        );
        assertSame(store, product.getStore());
    }


    @Test
    void shouldReturnEmptyWhenProductIsNotListed() {
        GroceryStore store = GroceryStore.builder()
                .outletId("store-a")
                .build();

        assertTrue(store.findProduct("missing").isEmpty());
    }

    @Test
    void shouldRejectDuplicateProductId() {
        GroceryStore store = GroceryStore.builder()
                .outletId("store-a")
                .build();

        GroceryProduct original = GroceryProduct.builder()
                .productId("product-a")
                .build();

        GroceryProduct duplicate = GroceryProduct.builder()
                .productId("product-a")
                .build();

        store.addProduct(original);

        assertThrows(
                IllegalArgumentException.class,
                () -> store.addProduct(duplicate)
        );

        assertEquals(1, store.getInventory().size());
        assertSame(
                original,
                store.findProduct("product-a").orElseThrow()
        );
    }
}