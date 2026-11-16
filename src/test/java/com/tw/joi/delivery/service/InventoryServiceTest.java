package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.dto.response.InventoryStockResponse;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryOutletRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    private InventoryService service;
    private ProductService productService;
    private GroceryProduct grocery;
    private FoodProduct meal;

    @BeforeEach
    void setUp() {
        GroceryStore store = GroceryStore.builder().outletId("store-a").build();
        Restaurant restaurant = Restaurant.builder().outletId("restaurant-a").build();
        grocery = GroceryProduct.builder().productId("grocery-a").availableStock(10).build();
        meal = FoodProduct.builder().productId("meal-a").availableStock(10).build();
        store.addProduct(grocery);
        restaurant.addProduct(meal);
        productService = new ProductService(new InMemoryOutletRepository(List.of(store, restaurant)));
        service = new InventoryService(productService);
    }

    @ParameterizedTest
    @CsvSource({
            "store-a, grocery-a, 0",
            "store-a, grocery-a, 40",
            "restaurant-a, meal-a, 0",
            "restaurant-a, meal-a, 40"
    })
    void shouldSetStockForBothProductTypesAndAllowRetry(String outletId, String productId, int stock) {
        InventoryStockResponse first = service.setStock(outletId, productId, stock);
        InventoryStockResponse retry = service.setStock(outletId, productId, stock);

        assertEquals(new InventoryStockResponse(outletId, productId, stock), first);
        assertEquals(first, retry);
        assertEquals(stock, productService.getProduct(productId, outletId).getAvailableStock());
        assertEquals(10, productId.equals("grocery-a") ? meal.getAvailableStock() : grocery.getAvailableStock());
    }

    @ParameterizedTest
    @CsvSource({
            "store-a, grocery-a, 3, 13",
            "store-a, grocery-a, -4, 6",
            "store-a, grocery-a, -10, 0",
            "restaurant-a, meal-a, 3, 13",
            "restaurant-a, meal-a, -4, 6",
            "restaurant-a, meal-a, -10, 0"
    })
    void shouldAdjustStockUpOrDownForBothProductTypes(
            String outletId, String productId, int delta, int expectedStock) {
        InventoryStockResponse response = service.adjustStock(outletId, productId, delta);

        assertEquals(new InventoryStockResponse(outletId, productId, expectedStock), response);
        assertEquals(expectedStock, productService.getProduct(productId, outletId).getAvailableStock());
    }

    @Test
    void shouldRejectNegativeTargetAndZeroAdjustmentWithoutMutation() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setStock("store-a", "grocery-a", -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.adjustStock("store-a", "grocery-a", 0));

        assertEquals(10, grocery.getAvailableStock());
    }

    @Test
    void shouldRejectWithdrawalExceedingStockWithoutMutation() {
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> service.adjustStock("store-a", "grocery-a", -11));

        assertEquals("Requested quantity 11 exceeds available stock 10 for product grocery-a",
                exception.getMessage());
        assertEquals(10, grocery.getAvailableStock());
    }

    @Test
    void shouldRejectOverflowWithoutWrappingOrMutation() {
        service.setStock("store-a", "grocery-a", Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class,
                () -> service.adjustStock("store-a", "grocery-a", 1));

        assertEquals(Integer.MAX_VALUE, grocery.getAvailableStock());
    }

    @Test
    void shouldHandleMinimumIntegerWithdrawalWithoutNegationOverflow() {
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> service.adjustStock("store-a", "grocery-a", Integer.MIN_VALUE));

        assertEquals("Requested quantity 2147483648 exceeds available stock 10 for product grocery-a",
                exception.getMessage());
        assertEquals(10, grocery.getAvailableStock());
    }

    @Test
    void shouldAllowAbsoluteRepairButRejectAdjustmentOfInvalidCatalogStock() {
        grocery.setAvailableStock(-1);

        assertThrows(IllegalStateException.class,
                () -> service.adjustStock("store-a", "grocery-a", 5));
        assertEquals(-1, grocery.getAvailableStock());

        InventoryStockResponse repaired = service.setStock("store-a", "grocery-a", 5);

        assertEquals(5, repaired.availableStock());
        assertEquals(5, grocery.getAvailableStock());
    }

    @Test
    void shouldRejectMissingOutletOrProductAndWrongOutlet() {
        assertThrows(StoreIdDoesNotExistException.class,
                () -> service.setStock("missing", "grocery-a", 5));
        assertThrows(ProductDoesNotExistException.class,
                () -> service.adjustStock("store-a", "missing", 1));
        assertThrows(ProductDoesNotExistException.class,
                () -> service.setStock("restaurant-a", "grocery-a", 5));

        assertEquals(10, grocery.getAvailableStock());
        assertEquals(10, meal.getAvailableStock());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldValidateIdsBeforeLookingUpProduct(String invalidId) {
        ProductService lookup = mock(ProductService.class);
        InventoryService inventory = new InventoryService(lookup);

        assertThrows(IllegalArgumentException.class, () -> inventory.setStock(invalidId, "product", 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.setStock("outlet", invalidId, 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.adjustStock(invalidId, "product", 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.adjustStock("outlet", invalidId, 1));

        verifyNoInteractions(lookup);
    }

    @Test
    void shouldNotLoseConcurrentIncrements() throws Exception {
        List<InventoryStockResponse> results = concurrently(8,
                () -> service.adjustStock("store-a", "grocery-a", 1));

        assertEquals(18, grocery.getAvailableStock());
        assertEquals(List.of(11, 12, 13, 14, 15, 16, 17, 18),
                results.stream().map(InventoryStockResponse::availableStock).sorted().toList());
    }

    @Test
    void shouldAllowOnlyOneConcurrentWithdrawalWhenStockCannotCoverBoth() throws Exception {
        List<Boolean> successes = concurrently(2, () -> {
            try {
                service.adjustStock("store-a", "grocery-a", -7);
                return true;
            } catch (InsufficientStockException exception) {
                return false;
            }
        });

        assertEquals(1, successes.stream().filter(Boolean::booleanValue).count());
        assertEquals(3, grocery.getAvailableStock());
    }

    private <T> List<T> concurrently(int callers, Callable<T> action) throws Exception {
        var executor = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < callers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return action.call();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
