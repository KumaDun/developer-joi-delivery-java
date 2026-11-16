package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.domain.StockStatus;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryOutletRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InventoryHealthResponseServiceTest {

    @ParameterizedTest
    @CsvSource({
            "0, 10, OUT_OF_STOCK",
            "1, 10, LOW_STOCK",
            "9, 10, LOW_STOCK",
            "10, 10, LOW_STOCK",
            "11, 10, HEALTHY",
            "0, 0, OUT_OF_STOCK",
            "1, 0, HEALTHY"
    })
    void shouldClassifyStockIncludingThresholdBoundaries(
            int stock, int threshold, StockStatus expectedStatus) {
        GroceryStore store = store("store-a");
        GroceryProduct product = product("product-a", stock, threshold);
        store.addProduct(product);

        InventoryHealthResponse health = service(store).getHealth("store-a");

        var item = health.products().getFirst();
        assertEquals("store-a", health.storeId());
        assertEquals("Store store-a", health.storeName());
        assertEquals("product-a", item.productId());
        assertEquals("Product product-a", item.productName());
        assertEquals(stock, item.availableStock());
        assertEquals(threshold, item.threshold());
        assertEquals(expectedStatus, item.status());
        assertEquals(stock, product.getAvailableStock());
        assertEquals(threshold, product.getThreshold());
    }

    @Test
    void shouldReturnOnlyRequestedStoresProductsSortedById() {
        GroceryStore first = store("store-a");
        GroceryStore second = store("store-b");
        first.addProduct(product("product-z", 30, 10));
        first.addProduct(product("product-a", 0, 10));
        second.addProduct(product("product-b", 3, 10));

        InventoryHealthResponse health = service(first, second).getHealth("store-a");

        assertEquals("store-a", health.storeId());
        assertEquals("Store store-a", health.storeName());
        assertEquals(List.of("product-a", "product-z"),
                health.products().stream().map(InventoryHealthResponse.ProductHealth::productId).toList());
        assertEquals(List.of(StockStatus.OUT_OF_STOCK, StockStatus.HEALTHY),
                health.products().stream().map(InventoryHealthResponse.ProductHealth::status).toList());
        assertEquals(2, first.getInventory().size());
        assertEquals(1, second.getInventory().size());
    }

    @Test
    void shouldReturnEmptyInventoryForExistingEmptyStore() {
        InventoryHealthResponse health = service(store("store-a")).getHealth("store-a");

        assertEquals("store-a", health.storeId());
        assertEquals("Store store-a", health.storeName());
        assertTrue(health.products().isEmpty());
    }

    @Test
    void shouldRejectUnknownStore() {
        var service = service(store("store-a"));

        assertThrows(StoreIdDoesNotExistException.class, () -> service.getHealth("missing"));
    }

    @Test
    void shouldRejectRestaurantWithoutGroceryThresholdRules() {
        Restaurant restaurant = Restaurant.builder().outletId("restaurant-a").build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(restaurant).getHealth("restaurant-a"));

        assertEquals("Inventory health is only available for grocery stores", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "1, -1", "-1, -1"})
    void shouldRejectNegativeCatalogValues(int stock, int threshold) {
        GroceryStore store = store("store-a");
        store.addProduct(product("product-a", stock, threshold));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service(store).getHealth("store-a"));

        assertEquals("Invalid inventory values for product: product-a", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldRejectMissingOrBlankStoreId(String storeId) {
        var service = service();

        assertThrows(IllegalArgumentException.class, () -> service.getHealth(storeId));
    }

    @Test
    void shouldKeepReadOnlyHealthSnapshotIndependentOfLaterCatalogChanges() {
        GroceryStore store = store("store-a");
        GroceryProduct product = product("product-a", 2, 10);
        store.addProduct(product);

        InventoryHealthResponse health = service(store).getHealth("store-a");
        store.setName("Changed store name");
        product.setProductName("Changed name");
        product.setAvailableStock(99);
        product.setThreshold(1);
        store.addProduct(product("product-b", 20, 10));

        assertEquals(1, health.products().size());
        assertEquals("Store store-a", health.storeName());
        var original = health.products().getFirst();
        assertEquals("Product product-a", original.productName());
        assertEquals(2, original.availableStock());
        assertEquals(10, original.threshold());
        assertEquals(StockStatus.LOW_STOCK, original.status());
        assertThrows(UnsupportedOperationException.class, () -> health.products().clear());
    }

    private GroceryStore store(String id) {
        return GroceryStore.builder().outletId(id).name("Store " + id).build();
    }

    private GroceryProduct product(String id, int stock, int threshold) {
        return GroceryProduct.builder()
                .productId(id)
                .productName("Product " + id)
                .availableStock(stock)
                .threshold(threshold)
                .build();
    }

    private InventoryHealthService service(Outlet<?>... outlets) {
        return new InventoryHealthService(new InMemoryOutletRepository(List.of(outlets)));
    }
}
