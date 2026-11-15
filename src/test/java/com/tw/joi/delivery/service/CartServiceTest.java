package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.response.CartProductInfo;
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
    private GroceryProduct product;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        productService = mock(ProductService.class);
        cartService = new CartService(userService, productService);

        // CartService currently reads this shared seeded cart directly.
        cart = SeedData.cartForUsers.get("user101");
        originalItems = cart.getCartItems();
        cart.setCartItems(new ArrayList<>());

        product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .sellingPrice(new BigDecimal("10.50"))
                .availableStock(5)
                .build();

        when(userService.fetchUserById("user101"))
                .thenReturn(SeedData.user101);
    }

    @AfterEach
    void restoreCart() {
        cart.setCartItems(originalItems);
    }

    @Test
    void shouldAddProductWithRequestedQuantityAndReturnDetails() {
        stubProductLookup();

        CartProductInfo result =
                cartService.addProductToCartForUser(request(2));

        assertEquals(1, cart.getCartItems().size());

        CartItem item = cart.getCartItems().getFirst();
        assertEquals("product101", item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("10.50"), item.getUnitPrice());
        assertEquals(new BigDecimal("21.00"), item.getTotalPrice());

        assertSame(cart, result.cart());
        assertSame(product, result.product());
        assertEquals(new BigDecimal("10.50"), result.sellingPrice());

        // Adding to the cart does not reserve stock.
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

        assertThrows(
                IllegalStateException.class,
                () -> cartService.addProductToCartForUser(request(6))
        );

        assertEquals(1, cart.getCartItems().size());
        assertSame(existing, cart.getCartItems().getFirst());
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
    }

    @Test
    void shouldReturnUsersCart() {
        Cart result = cartService.getCartForUser("user101");

        assertSame(cart, result);
        verify(userService).fetchUserById("user101");
    }

    private void stubProductLookup() {
        when(productService.getProduct("product101", "store101"))
                .thenReturn(product);
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