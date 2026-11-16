package com.tw.joi.delivery.domain;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertSame(store, product.getOutlet());
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

    @Test
    void shouldRejectMovingProductToAnotherStore() {
        GroceryStore first = GroceryStore.builder().outletId("store-a").build();
        GroceryStore second = GroceryStore.builder().outletId("store-b").build();
        GroceryProduct product = GroceryProduct.builder().productId("product-a").build();
        first.addProduct(product);

        assertThrows(IllegalArgumentException.class, () -> second.addProduct(product));

        assertSame(first, product.getStore());
        assertSame(first, product.getOutlet());
        assertEquals(Set.of(product), first.getInventory());
        assertTrue(second.getInventory().isEmpty());
    }

    @Test
    void shouldAcceptSeparateProductInstancesWithDistinctIdsAtDifferentStores() {
        GroceryStore first = GroceryStore.builder().outletId("store-a").build();
        GroceryStore second = GroceryStore.builder().outletId("store-b").build();
        GroceryProduct firstProduct = GroceryProduct.builder().productId("product-a").build();
        GroceryProduct secondProduct = GroceryProduct.builder().productId("product-b").build();

        first.addProduct(firstProduct);
        second.addProduct(secondProduct);

        assertSame(firstProduct, first.findProduct("product-a").orElseThrow());
        assertSame(secondProduct, second.findProduct("product-b").orElseThrow());
        assertSame(first, firstProduct.getOutlet());
        assertSame(second, secondProduct.getOutlet());
    }

    @Test
    void shouldRejectExistingStoreAliasEvenIfInheritedOutletIsMissing() {
        GroceryStore owner = GroceryStore.builder().outletId("store-a").build();
        GroceryStore other = GroceryStore.builder().outletId("store-b").build();
        GroceryProduct product = GroceryProduct.builder().productId("product-a").build();
        product.setStore(owner);

        assertThrows(IllegalArgumentException.class, () -> other.addProduct(product));

        assertSame(owner, product.getStore());
        assertNull(product.getOutlet());
        assertTrue(other.getInventory().isEmpty());
    }

    @Test
    void shouldRejectProductConstructedWithAnotherStore() {
        GroceryStore owner = GroceryStore.builder().outletId("store-a").build();
        GroceryStore other = GroceryStore.builder().outletId("store-b").build();
        GroceryProduct product = GroceryProduct.builder()
                .productId("product-a").store(owner).build();

        assertThrows(IllegalArgumentException.class, () -> other.addProduct(product));

        assertSame(owner, product.getOutlet());
        assertTrue(other.getInventory().isEmpty());
    }

    @Test
    void shouldExposeInventoryAsReadOnlySnapshot() {
        GroceryStore store = GroceryStore.builder().outletId("store-a").build();
        Set<GroceryProduct> emptyInventory = store.getInventory();
        GroceryProduct product = GroceryProduct.builder().productId("product-a").build();

        assertThrows(UnsupportedOperationException.class, () -> emptyInventory.add(product));
        store.addProduct(product);

        assertTrue(emptyInventory.isEmpty());
        assertEquals(Set.of(product), store.getInventory());
        assertThrows(UnsupportedOperationException.class, () -> store.getInventory().clear());
        assertSame(store, product.getOutlet());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectInvalidProductIdBeforeChangingOwnership(String productId) {
        GroceryStore store = GroceryStore.builder().outletId("store-a").build();
        GroceryProduct product = GroceryProduct.builder().productId(productId).build();

        assertThrows(IllegalArgumentException.class, () -> store.addProduct(product));

        assertTrue(store.getInventory().isEmpty());
        assertNull(product.getStore());
        assertNull(product.getOutlet());
    }

    @Test
    void shouldRejectNullProductAndMissingOutletId() {
        GroceryStore store = GroceryStore.builder().outletId("store-a").build();
        assertThrows(IllegalArgumentException.class, () -> store.addProduct(null));

        GroceryStore missingId = new GroceryStore();
        GroceryProduct product = GroceryProduct.builder().productId("product-a").build();
        assertThrows(IllegalArgumentException.class, () -> missingId.addProduct(product));

        assertTrue(missingId.getInventory().isEmpty());
        assertNull(product.getStore());
        assertNull(product.getOutlet());
    }
}
