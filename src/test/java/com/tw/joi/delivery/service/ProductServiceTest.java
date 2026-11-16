package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryOutletRepository;
import com.tw.joi.delivery.repository.StoreRepository;
import com.tw.joi.delivery.seedData.SeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

    private ProductService productService;
    private StoreRepository storeRepository;

    @BeforeEach
    void setUp() {
        SeedData seedData = SeedData.create();
        storeRepository = new InMemoryOutletRepository(seedData.getOutlets());

        productService = new ProductService(storeRepository);
    }

    @Test
    void shouldReturnProductFromStoreInventory() {
        Product product =
                productService.getProduct("product101", "store101");

        GroceryProduct grocery =
                assertInstanceOf(GroceryProduct.class, product);

        assertEquals("product101", grocery.getProductId());
        assertEquals("store101", grocery.getStore().getOutletId());
        GroceryStore store = assertInstanceOf(GroceryStore.class,
                storeRepository.findById("store101").orElseThrow());
        assertSame(store, grocery.getStore());
        assertTrue(store.getInventory().contains(grocery));
    }

    @Test
    void shouldReturnMealFromRestaurantMenu() {
        Product product = productService.getProduct("meal101", "restaurant101");

        FoodProduct meal = assertInstanceOf(FoodProduct.class, product);

        assertEquals("Chicken Curry", meal.getProductName());
        Restaurant restaurant = assertInstanceOf(Restaurant.class,
                storeRepository.findById("restaurant101").orElseThrow());
        assertSame(restaurant, meal.getOutlet());
        assertTrue(restaurant.getMenu().contains(meal));
    }

    @Test
    void shouldFindMealWithoutSpecifyingOutlet() {
        Product meal = productService.getProduct("meal102");

        assertInstanceOf(FoodProduct.class, meal);
        assertEquals("Vegetable Curry", meal.getProductName());
        assertEquals("restaurant101", meal.getOutlet().getOutletId());
    }

    @Test
    void shouldRejectMealRequestedFromGroceryStore() {
        assertThrows(ProductDoesNotExistException.class,
                () -> productService.getProduct("meal101", "store101"));
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
