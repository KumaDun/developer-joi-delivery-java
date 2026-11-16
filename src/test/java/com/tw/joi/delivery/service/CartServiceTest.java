package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.domain.SpiceLevel;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.CartItemDoesNotExistException;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.repository.CartRepository;
import com.tw.joi.delivery.repository.InMemoryCartRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceTest {

    private UserService userService;
    private ProductService productService;
    private CartService cartService;

    private Cart cart;
    private CartRepository cartRepository;
    private User user;
    private GroceryProduct product;
    private GroceryStore store;
    private Restaurant restaurant;
    private FoodProduct meal;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        productService = mock(ProductService.class);
        cartRepository = new InMemoryCartRepository();
        cartService = new CartService(userService, productService, cartRepository);
        user = User.builder().userId("user101").build();
        cart = cartRepository.getOrCreateForUser(user);

        store = GroceryStore.builder().outletId("store101").name("Fresh Picks").build();
        product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .sellingPrice(new BigDecimal("10.50"))
                .availableStock(5)
                .build();
        store.addProduct(product);

        restaurant = Restaurant.builder()
                .outletId("restaurant101").name("Spice Kitchen").build();
        meal = FoodProduct.builder()
                .productId("meal101").productName("Chicken Curry")
                .unitPrice(new BigDecimal("25.00")).availableStock(5).build();
        restaurant.addProduct(meal);

        when(userService.fetchUserById("user101"))
                .thenReturn(user);
    }

    @Test
    void shouldAddProductWithRequestedQuantityAndReturnDetails() {
        stubProductLookup();

        CartItem result =
                cartService.addProductToCartForUser(request(2));

        // Verify the actual cart was updated.
        assertEquals(1, cart.getCartItems().size());

        CartItem item = cart.getCartItems().getFirst();
        assertSame(item, result);
        assertSame(store, cart.getOutlet());
        assertEquals("product101", item.getProductId());
        assertEquals("Wheat Bread", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("10.50"), item.getUnitPrice());
        assertEquals(new BigDecimal("21.00"), item.getTotalPrice());

        // Adding to a cart does not reserve stock.
        assertEquals(5, product.getAvailableStock());
    }

    @Test
    void shouldAllowQuantityEqualToAvailableStock() {
        stubProductLookup();

        cartService.addProductToCartForUser(request(5));

        assertEquals(5, cart.getCartItems().getFirst().getQuantity());
    }

    @Test
    void shouldRejectInsufficientStockAndPreserveExistingItems() {
        stubProductLookup();

        CartItem existing = new CartItem(product, 1);
        cart.getCartItems().add(existing);
        cart.setOutlet(store);

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addProductToCartForUser(request(6))
        );

        assertEquals(1, cart.getCartItems().size());
        assertSame(existing, cart.getCartItems().getFirst());
        assertSame(store, cart.getOutlet());
        assertEquals(5, product.getAvailableStock());
    }

    @Test
    void shouldRejectWhenCombinedQuantityExceedsStock() {
        stubProductLookup();

        cartService.addProductToCartForUser(request(3));

        CartItem existing = cart.getCartItems().getFirst();

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addProductToCartForUser(request(3))
        );

        assertEquals(1, cart.getCartItems().size());
        assertSame(existing, cart.getCartItems().getFirst());
        assertEquals(3, existing.getQuantity());
        assertEquals(5, product.getAvailableStock());
    }

    @Test
    void shouldAllowCombinedQuantityEqualToStock() {
        stubProductLookup();

        cartService.addProductToCartForUser(request(3));
        cartService.addProductToCartForUser(request(2));

        long totalQuantity = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals("product101"))
                .mapToLong(CartItem::getQuantity)
                .sum();

        assertEquals(5L, totalQuantity);
        assertEquals(5, product.getAvailableStock());
    }

    @Test
    void shouldRejectOutOfStockProduct() {
        stubProductLookup();
        product.setAvailableStock(0);

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addProductToCartForUser(request(1))
        );

        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveQuantity(int quantity) {
        stubProductLookup();

        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addProductToCartForUser(request(quantity))
        );

        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
    }

    @Test
    void shouldPreserveCartWhenProductLookupFails() {
        when(productService.getProduct("product101", "store101"))
                .thenThrow(
                        new ProductDoesNotExistException("product101", "store101")
                );

        assertThrows(
                ProductDoesNotExistException.class,
                () -> cartService.addProductToCartForUser(request(1))
        );

        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
    }

    @Test
    void shouldReturnUsersCart() {
        cart.getCartItems().add(new CartItem(product, 1));
        cart.setOutlet(store);

        Cart result = cartService.getCartForUser("user101");

        assertSame(cart, result);
        verify(userService).fetchUserById("user101");
    }

    private void stubProductLookup() {
        when(productService.getProduct("product101", "store101"))
                .thenReturn(product);
    }

    @Test
    void shouldReturnCartSnapshotWithQuantityAndPricesWithoutCatalogLookups() {
        cart.getCartItems().add(new CartItem(product, 2));
        cart.setOutlet(store);

        Cart result = cartService.getCartSnapshotForUser("user101");

        assertNotSame(cart, result);
        assertEquals(cart.getCartId(), result.getCartId());
        assertEquals("store101", result.getOutlet().getOutletId());
        assertEquals("user101", result.getUser().getUserId());
        assertEquals(1, result.getCartItems().size());

        CartItem returnedItem = result.getCartItems().getFirst();

        assertEquals("product101", returnedItem.getProductId());
        assertEquals(2, returnedItem.getQuantity());
        assertEquals(new BigDecimal("10.50"), returnedItem.getUnitPrice());
        assertEquals(new BigDecimal("21.00"), returnedItem.getTotalPrice());

        assertEquals(1, cart.getCartItems().size());
        assertEquals(5, product.getAvailableStock());
        verifyNoInteractions(productService);
    }

    @Test
    void shouldAddMealWithRequestedCustomizationAndBindRestaurant() {
        stubMealLookup();
        AddProductRequest request = mealRequest(2);
        request.setSpiceLevel(SpiceLevel.MEDIUM);
        request.setUtensilsNeeded(true);

        CartItem item = cartService.addProductToCartForUser(request);

        assertSame(restaurant, cart.getOutlet());
        assertSame(item, cart.getCartItems().getFirst());
        assertEquals(SpiceLevel.MEDIUM, item.getSpiceLevel());
        assertEquals(Boolean.TRUE, item.getUtensilsNeeded());
        assertEquals(new BigDecimal("50.00"), item.getTotalPrice());
    }

    @Test
    void shouldDefaultMealCustomizationWhenOmitted() {
        stubMealLookup();

        CartItem item = cartService.addProductToCartForUser(mealRequest(1));

        assertEquals(SpiceLevel.NONE, item.getSpiceLevel());
        assertEquals(Boolean.FALSE, item.getUtensilsNeeded());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRejectUtensilsOptionForGroceriesWithoutChangingEmptyCart(boolean utensils) {
        stubProductLookup();
        cart.setOutlet(restaurant);
        AddProductRequest request = request(1);
        request.setUtensilsNeeded(utensils);

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addProductToCartForUser(request));

        assertTrue(cart.getCartItems().isEmpty());
        assertSame(restaurant, cart.getOutlet());
    }

    @Test
    void shouldRejectSpiceOptionForGroceriesWithoutChangingExistingItems() {
        stubProductLookup();
        CartItem existing = new CartItem(product, 1);
        cart.getCartItems().add(existing);
        cart.setOutlet(store);
        AddProductRequest request = request(1);
        request.setSpiceLevel(SpiceLevel.NONE);

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addProductToCartForUser(request));

        assertEquals(List.of(existing), cart.getCartItems());
        assertSame(store, cart.getOutlet());
    }

    @Test
    void shouldRebindPreviouslyUsedEmptyCartOnSuccessfulAddition() {
        stubMealLookup();
        cart.setOutlet(store);

        cartService.addProductToCartForUser(mealRequest(1));

        assertSame(restaurant, cart.getOutlet());
        assertEquals("meal101", cart.getCartItems().getFirst().getProductId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRejectMixingMealsAndGroceriesWithoutChangingCart(boolean mealFirst) {
        stubProductLookup();
        stubMealLookup();
        CartItem existing = cartService.addProductToCartForUser(
                mealFirst ? mealRequest(1) : request(1));
        Outlet<?> existingOutlet = cart.getOutlet();

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addProductToCartForUser(
                        mealFirst ? request(1) : mealRequest(1)));

        assertEquals(List.of(existing), cart.getCartItems());
        assertSame(existingOutlet, cart.getOutlet());
    }

    @Test
    void shouldRejectAddingFromAnotherGroceryStore() {
        stubProductLookup();
        CartItem existing = cartService.addProductToCartForUser(request(1));
        GroceryStore otherStore = GroceryStore.builder().outletId("store102").build();
        GroceryProduct otherProduct = GroceryProduct.builder()
                .productId("product102").productName("Spinach")
                .sellingPrice(BigDecimal.ONE).availableStock(5).build();
        otherStore.addProduct(otherProduct);
        when(productService.getProduct("product102", "store102")).thenReturn(otherProduct);
        AddProductRequest request = request(1);
        request.setProductId("product102");
        request.setOutletId("store102");

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addProductToCartForUser(request));

        assertEquals(List.of(existing), cart.getCartItems());
        assertSame(store, cart.getOutlet());
    }

    @Test
    void shouldRejectProductBelongingToAnotherOutletBeforeBindingCart() {
        when(productService.getProduct("meal101", "store101")).thenReturn(meal);
        AddProductRequest request = request(1);
        request.setProductId("meal101");

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addProductToCartForUser(request));

        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
    }

    @Test
    void shouldKeepCustomizationRowsSeparateAndCheckCombinedStock() {
        stubMealLookup();
        AddProductRequest mild = mealRequest(2);
        mild.setSpiceLevel(SpiceLevel.MILD);
        mild.setUtensilsNeeded(false);
        AddProductRequest hot = mealRequest(3);
        hot.setSpiceLevel(SpiceLevel.HOT);
        hot.setUtensilsNeeded(true);

        CartItem first = cartService.addProductToCartForUser(mild);
        CartItem second = cartService.addProductToCartForUser(hot);

        assertEquals(List.of(first, second), cart.getCartItems());
        assertEquals(SpiceLevel.MILD, first.getSpiceLevel());
        assertEquals(Boolean.FALSE, first.getUtensilsNeeded());
        assertEquals(SpiceLevel.HOT, second.getSpiceLevel());
        assertEquals(Boolean.TRUE, second.getUtensilsNeeded());
        assertEquals(5, first.getQuantity() + second.getQuantity());
        assertThrows(InsufficientStockException.class,
                () -> cartService.addProductToCartForUser(mealRequest(1)));
        assertEquals(List.of(first, second), cart.getCartItems());
        assertSame(restaurant, cart.getOutlet());
        assertEquals(5, meal.getAvailableStock());
    }

    @Test
    void shouldCreateCartForValidUserWithoutAnySeededCart() {
        User newUser = User.builder().userId("new-user").firstName("New").build();
        when(userService.fetchUserById("new-user")).thenReturn(newUser);

        Cart first = cartService.getCartForUser("new-user");
        Cart second = cartService.getCartForUser("new-user");

        assertSame(first, second);
        assertSame(newUser, first.getUser());
        assertFalse(first.getCartId().isBlank());
        assertTrue(first.getCartItems().isEmpty());
        assertNull(first.getOutlet());
    }

    @Test
    void shouldValidateUserBeforeCreatingCart() {
        CartRepository repository = mock(CartRepository.class);
        CartService service = new CartService(userService, productService, repository);
        when(userService.fetchUserById("missing"))
                .thenThrow(new UserDoesNotExistException("missing"));

        assertThrows(UserDoesNotExistException.class,
                () -> service.getCartForUser("missing"));

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnSnapshotUnaffectedByLaterCartChanges() {
        stubProductLookup();
        CartItem firstItem = cartService.addProductToCartForUser(request(1));

        Cart snapshot = cartService.getCartSnapshotForUser("user101");
        cartService.addProductToCartForUser(request(1));
        synchronized (cart) {
            cart.getCartItems().clear();
            cart.setOutlet(restaurant);
        }

        assertNotSame(cart, snapshot);
        assertEquals(List.of(firstItem), snapshot.getCartItems());
        assertSame(store, snapshot.getOutlet());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getCartItems().clear());
    }

    @Test
    void shouldSerializeConcurrentAddsBeforeCheckingCombinedStock() throws Exception {
        when(productService.getProduct("product101", "store101")).thenAnswer(invocation -> {
            assertTrue(Thread.holdsLock(cart), "Lookup and stock validation must hold the canonical cart lock");
            return product;
        });

        List<RuntimeException> failures = attemptConcurrently(request(3), request(3));

        assertEquals(1, failures.size());
        assertInstanceOf(InsufficientStockException.class, failures.getFirst());
        assertEquals("Requested quantity 6 exceeds available stock 5 for product product101",
                failures.getFirst().getMessage());
        assertEquals(1, cart.getCartItems().size());
        assertEquals(3, cart.getCartItems().getFirst().getQuantity());
        assertSame(store, cart.getOutlet());
    }

    @Test
    void shouldKeepOneOutletWhenFirstAddsArriveConcurrently() throws Exception {
        when(productService.getProduct("product101", "store101")).thenAnswer(invocation -> {
            assertTrue(Thread.holdsLock(cart));
            return product;
        });
        when(productService.getProduct("meal101", "restaurant101")).thenAnswer(invocation -> {
            assertTrue(Thread.holdsLock(cart));
            return meal;
        });

        List<RuntimeException> failures = attemptConcurrently(request(1), mealRequest(1));

        assertEquals(1, failures.size());
        assertInstanceOf(IllegalArgumentException.class, failures.getFirst());
        assertEquals(1, cart.getCartItems().size());
        CartItem item = cart.getCartItems().getFirst();
        assertSame(item.getProductId().equals("meal101") ? restaurant : store, cart.getOutlet());
    }

    @Test
    void shouldUpdateOnlySelectedCustomizedLineAndPreserveCapturedValues() {
        CartItem mild = new CartItem(meal, 2, meal.getUnitPrice(), SpiceLevel.MILD, false);
        CartItem hot = new CartItem(meal, 1, meal.getUnitPrice(), SpiceLevel.HOT, true);
        cart.getCartItems().addAll(List.of(mild, hot));
        cart.setOutlet(restaurant);
        stubMealLookup();
        meal.setProductName("New catalog name");
        meal.setUnitPrice(new BigDecimal("30.00"));

        Cart result = cartService.updateCartItemQuantity("user101", mild.getCartItemId(), 3);

        assertNotEquals(mild.getCartItemId(), hot.getCartItemId());
        assertNotSame(cart, result);
        CartItem changed = result.getCartItems().getFirst();
        assertEquals(mild.getCartItemId(), changed.getCartItemId());
        assertEquals("Chicken Curry", changed.getProductName());
        assertEquals(3, changed.getQuantity());
        assertEquals(new BigDecimal("25.00"), changed.getUnitPrice());
        assertEquals(new BigDecimal("75.00"), changed.getTotalPrice());
        assertEquals(SpiceLevel.MILD, changed.getSpiceLevel());
        assertEquals(Boolean.FALSE, changed.getUtensilsNeeded());
        assertSame(hot, result.getCartItems().get(1));
        assertEquals(SpiceLevel.HOT, hot.getSpiceLevel());
        assertEquals(Boolean.TRUE, hot.getUtensilsNeeded());
        assertEquals(2, mild.getQuantity());
        assertEquals(5, meal.getAvailableStock());
        assertThrows(UnsupportedOperationException.class, () -> result.getCartItems().clear());

        cartService.updateCartItemQuantity("user101", mild.getCartItemId(), 1);
        assertEquals(3, result.getCartItems().getFirst().getQuantity());
    }

    @Test
    void shouldSetAbsoluteQuantityAndTreatSameQuantityRetryAsNoOp() {
        CartItem original = new CartItem(product, 2);
        cart.getCartItems().add(original);
        cart.setOutlet(store);
        stubProductLookup();

        Cart increased = cartService.updateCartItemQuantity("user101", original.getCartItemId(), 3);
        CartItem updated = increased.getCartItems().getFirst();
        clearInvocations(productService);

        Cart retry = cartService.updateCartItemQuantity("user101", original.getCartItemId(), 3);
        Cart decreased = cartService.updateCartItemQuantity("user101", original.getCartItemId(), 1);

        assertSame(updated, retry.getCartItems().getFirst());
        assertEquals(3, retry.getCartItems().getFirst().getQuantity());
        assertEquals(1, decreased.getCartItems().getFirst().getQuantity());
        assertEquals(original.getCartItemId(), decreased.getCartItems().getFirst().getCartItemId());
        verifyNoInteractions(productService);
    }

    @Test
    void shouldRemoveOnlySelectedLineAndClearOutletAfterLastRemoval() {
        CartItem first = new CartItem(product, 2);
        CartItem second = new CartItem(product, 1);
        cart.getCartItems().addAll(List.of(first, second));
        cart.setOutlet(store);

        Cart oneLeft = cartService.updateCartItemQuantity("user101", first.getCartItemId(), 0);
        Cart empty = cartService.updateCartItemQuantity("user101", second.getCartItemId(), 0);

        assertEquals(List.of(second), oneLeft.getCartItems());
        assertSame(store, oneLeft.getOutlet());
        assertTrue(empty.getCartItems().isEmpty());
        assertNull(empty.getOutlet());
        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
        assertThrows(CartItemDoesNotExistException.class,
                () -> cartService.updateCartItemQuantity("user101", second.getCartItemId(), 0));
        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectNegativeQuantityOrUnknownItemWithoutChangingCart() {
        CartItem original = new CartItem(product, 2);
        cart.getCartItems().add(original);
        cart.setOutlet(store);

        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateCartItemQuantity("user101", original.getCartItemId(), -1));
        assertThrows(CartItemDoesNotExistException.class,
                () -> cartService.updateCartItemQuantity("user101", "missing-item", 3));

        assertEquals(List.of(original), cart.getCartItems());
        assertSame(store, cart.getOutlet());
        verifyNoInteractions(productService);
    }

    @Test
    void shouldNotUpdateAnotherUsersItem() {
        CartItem original = new CartItem(product, 2);
        cart.getCartItems().add(original);
        cart.setOutlet(store);
        User otherUser = User.builder().userId("user102").build();
        when(userService.fetchUserById("user102")).thenReturn(otherUser);

        assertThrows(CartItemDoesNotExistException.class,
                () -> cartService.updateCartItemQuantity("user102", original.getCartItemId(), 3));

        assertEquals(List.of(original), cart.getCartItems());
        assertSame(store, cart.getOutlet());
        Cart otherCart = cartRepository.getOrCreateForUser(otherUser);
        assertTrue(otherCart.getCartItems().isEmpty());
        assertNull(otherCart.getOutlet());
        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectIncreaseExceedingStockAcrossCustomizationLines() {
        CartItem mild = new CartItem(meal, 3, meal.getUnitPrice(), SpiceLevel.MILD, false);
        CartItem hot = new CartItem(meal, 2, meal.getUnitPrice(), SpiceLevel.HOT, true);
        cart.getCartItems().addAll(List.of(mild, hot));
        cart.setOutlet(restaurant);
        stubMealLookup();

        InsufficientStockException failure = assertThrows(InsufficientStockException.class,
                () -> cartService.updateCartItemQuantity("user101", mild.getCartItemId(), 4));

        assertEquals("Requested quantity 6 exceeds available stock 5 for product meal101", failure.getMessage());
        assertEquals(List.of(mild, hot), cart.getCartItems());
        assertSame(restaurant, cart.getOutlet());
        assertEquals(5, meal.getAvailableStock());
    }

    @Test
    void shouldPermitDecreaseAndRemovalWithoutCatalogLookupAfterStockDrops() {
        CartItem first = new CartItem(product, 3);
        CartItem second = new CartItem(product, 2);
        cart.getCartItems().addAll(List.of(first, second));
        cart.setOutlet(store);
        product.setAvailableStock(0);
        when(productService.getProduct("product101", "store101"))
                .thenThrow(new ProductDoesNotExistException("product101", "store101"));

        Cart decreased = cartService.updateCartItemQuantity("user101", first.getCartItemId(), 2);
        Cart removed = cartService.updateCartItemQuantity("user101", first.getCartItemId(), 0);

        assertEquals(4, decreased.getCartItems().stream().mapToInt(CartItem::getQuantity).sum());
        assertEquals(first.getCartItemId(), decreased.getCartItems().getFirst().getCartItemId());
        assertEquals(List.of(second), removed.getCartItems());
        assertSame(store, removed.getOutlet());
        verifyNoInteractions(productService);
    }

    @Test
    void shouldPreserveCartWhenIncreaseCannotFindCatalogProduct() {
        CartItem original = new CartItem(product, 1);
        cart.getCartItems().add(original);
        cart.setOutlet(store);
        when(productService.getProduct("product101", "store101"))
                .thenThrow(new ProductDoesNotExistException("product101", "store101"));

        assertThrows(ProductDoesNotExistException.class,
                () -> cartService.updateCartItemQuantity("user101", original.getCartItemId(), 2));

        assertEquals(List.of(original), cart.getCartItems());
        assertSame(store, cart.getOutlet());
    }

    @Test
    void shouldRejectIncreaseWhenProductBelongsToAnotherOutlet() {
        CartItem original = new CartItem(product, 1);
        cart.getCartItems().add(original);
        cart.setOutlet(store);
        product.setOutlet(GroceryStore.builder().outletId("store102").build());
        stubProductLookup();

        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateCartItemQuantity("user101", original.getCartItemId(), 2));

        assertEquals(List.of(original), cart.getCartItems());
        assertSame(store, cart.getOutlet());
    }

    @Test
    void shouldRejectLargeAggregateIncreaseWithoutIntegerOverflow() {
        product.setAvailableStock(Integer.MAX_VALUE);
        CartItem first = new CartItem(product, Integer.MAX_VALUE - 1);
        CartItem second = new CartItem(product, 1);
        cart.getCartItems().addAll(List.of(first, second));
        cart.setOutlet(store);
        stubProductLookup();

        InsufficientStockException failure = assertThrows(InsufficientStockException.class,
                () -> cartService.updateCartItemQuantity("user101", second.getCartItemId(), Integer.MAX_VALUE));

        long total = (long) Integer.MAX_VALUE - 1 + Integer.MAX_VALUE;
        assertTrue(failure.getMessage().contains(Long.toString(total)));
        assertEquals(List.of(first, second), cart.getCartItems());
    }

    @Test
    void shouldCoordinateConcurrentQuantityIncreasesWithBothLocksAndCombinedStock() throws Exception {
        CartItem first = new CartItem(product, 1);
        CartItem second = new CartItem(product, 1);
        cart.getCartItems().addAll(List.of(first, second));
        cart.setOutlet(store);
        GroceryProduct lockedProduct = spy(product);
        when(productService.getProduct("product101", "store101")).thenAnswer(invocation -> {
            assertTrue(Thread.holdsLock(cart));
            return lockedProduct;
        });
        doAnswer(invocation -> {
            assertTrue(Thread.holdsLock(cart));
            assertTrue(Thread.holdsLock(lockedProduct));
            return invocation.callRealMethod();
        }).when(lockedProduct).getAvailableStock();

        List<RuntimeException> failures = attemptConcurrently(
                () -> cartService.updateCartItemQuantity("user101", first.getCartItemId(), 3),
                () -> cartService.updateCartItemQuantity("user101", second.getCartItemId(), 3));

        assertEquals(1, failures.size());
        assertInstanceOf(InsufficientStockException.class, failures.getFirst());
        assertEquals(List.of(1, 3), cart.getCartItems().stream().map(CartItem::getQuantity).sorted().toList());
        assertEquals(first.getCartItemId(), cart.getCartItems().getFirst().getCartItemId());
        assertEquals(second.getCartItemId(), cart.getCartItems().get(1).getCartItemId());
        assertEquals(1, first.getQuantity());
        assertEquals(1, second.getQuantity());
        assertSame(store, cart.getOutlet());
    }

    private List<RuntimeException> attemptConcurrently(
            AddProductRequest first, AddProductRequest second) throws Exception {
        return attemptConcurrently(
                () -> cartService.addProductToCartForUser(first),
                () -> cartService.addProductToCartForUser(second));
    }

    private List<RuntimeException> attemptConcurrently(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<RuntimeException>> results = List.of(
                    executor.submit(() -> attemptOperation(first, ready, start)),
                    executor.submit(() -> attemptOperation(second, ready, start)));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<RuntimeException> failures = new ArrayList<>();
            for (Future<RuntimeException> result : results) {
                RuntimeException failure = result.get(5, TimeUnit.SECONDS);
                if (failure != null) {
                    failures.add(failure);
                }
            }
            return failures;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private RuntimeException attemptOperation(Runnable operation,
                                             CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            operation.run();
            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private void stubMealLookup() {
        when(productService.getProduct("meal101", "restaurant101")).thenReturn(meal);
    }

    @Test
    void shouldReadInventoryUnderTheSameProductLockAsStockUpdates() {
        GroceryProduct lockedProduct = spy(product);
        when(productService.getProduct("product101", "store101")).thenReturn(lockedProduct);
        doAnswer(invocation -> {
            assertTrue(Thread.holdsLock(lockedProduct));
            assertTrue(Thread.holdsLock(cart));
            return invocation.callRealMethod();
        }).when(lockedProduct).getAvailableStock();

        CartItem item = cartService.addProductToCartForUser(request(2));

        assertEquals(2, item.getQuantity());
    }

    private AddProductRequest mealRequest(int quantity) {
        AddProductRequest request = request(quantity);
        request.setProductId("meal101");
        request.setOutletId("restaurant101");
        return request;
    }

    private AddProductRequest request(int quantity) {
        AddProductRequest request = new AddProductRequest();
        request.setUserId("user101");
        request.setOutletId("store101");
        request.setProductId("product101");
        request.setQuantity(quantity);
        return request;
    }
}
