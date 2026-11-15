package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryGroceryStoreRepository;
import com.tw.joi.delivery.repository.StoreRepository;
import com.tw.joi.delivery.seedData.SeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

    private ProductService productService;

    @BeforeEach
    void setUp() {
        StoreRepository storeRepository =
                new InMemoryGroceryStoreRepository(SeedData.groceryStores);

        productService = new ProductService(storeRepository);
    }

    @Test
    void shouldReturnProductFromStoreInventory() {
        GroceryProduct product =
                productService.getProduct("product101", "store101");

        assertNotNull(product);
        assertEquals("product101", product.getProductId());
        assertEquals("store101", product.getStore().getOutletId());
        assertTrue(SeedData.store101.getInventory().contains(product));
    }

    @Test
    void shouldThrowWhenProductIsNotInRequestedStore() {
        // product101 exists in store101, but not in store102.
        ProductDoesNotExistException exception = assertThrows(
                ProductDoesNotExistException.class,
                () -> productService.getProduct("product101", "store102")
        );

        assertEquals(
                "Product: product101 does not exist in store: store102",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenStoreDoesNotExist() {
        StoreIdDoesNotExistException exception = assertThrows(
                StoreIdDoesNotExistException.class,
                () -> productService.getProduct("product101", "missing-store")
        );

        assertEquals(
                "Store: missing-store does not exist",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        ProductDoesNotExistException exception = assertThrows(
                ProductDoesNotExistException.class,
                () -> productService.getProduct("product_missing")
        );

        assertEquals(
                "Product: product_missing does not exist",
                exception.getMessage()
        );
    }
}