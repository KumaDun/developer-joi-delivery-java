package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.domain.SpiceLevel;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
    private List<CartItem> originalItems;
    private Outlet<?> originalOutlet;
    private GroceryProduct product;
    private GroceryStore store;
    private Restaurant restaurant;
    private FoodProduct meal;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        productService = mock(ProductService.class);
        cartService = new CartService(userService, productService);

        // CartService currently reads this shared seeded cart directly.
        cart = SeedData.cartForUsers.get("user101");
        originalItems = cart.getCartItems();
        originalOutlet = cart.getOutlet();
        cart.setCartItems(new ArrayList<>());
        cart.setOutlet(null);

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
                .thenReturn(SeedData.user101);
    }

    @AfterEach
    void restoreCart() {
        cart.setCartItems(originalItems);
        cart.setOutlet(originalOutlet);
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
                IllegalStateException.class,
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
                IllegalStateException.class,
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
                IllegalStateException.class,
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
    void shouldReturnCartWithQuantityAndPricesWithoutCatalogLookups() {
        cart.getCartItems().add(new CartItem(product, 2));
        cart.setOutlet(store);

        Cart result = cartService.getCartForUser("user101");

        assertEquals("cart101", result.getCartId());
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
        assertThrows(IllegalStateException.class,
                () -> cartService.addProductToCartForUser(mealRequest(1)));
        assertEquals(List.of(first, second), cart.getCartItems());
        assertSame(restaurant, cart.getOutlet());
        assertEquals(5, meal.getAvailableStock());
    }

    private void stubMealLookup() {
        when(productService.getProduct("meal101", "restaurant101")).thenReturn(meal);
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
