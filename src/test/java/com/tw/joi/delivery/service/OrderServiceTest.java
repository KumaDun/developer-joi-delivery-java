package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.*;
import com.tw.joi.delivery.exception.OrderDoesNotExistException;
import com.tw.joi.delivery.exception.EmptyCartException;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.repository.InMemoryOrderRepository;
import com.tw.joi.delivery.repository.OrderRepository;
import com.tw.joi.delivery.repository.UserRepository;
import com.tw.joi.delivery.repository.InMemoryUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-12T10:00:00Z");

    private CartService cartService;
    private ProductService productService;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private UserRepository userRepository;
    private User user;

    private GroceryProduct product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        productService = mock(ProductService.class);
        orderRepository = spy(new InMemoryOrderRepository());
        user = User.builder().userId("user101").build();
        userRepository = spy(new InMemoryUserRepository(List.of(user)));

        orderService = new OrderService(
                cartService,
                productService,
                orderRepository,
                new UserService(userRepository),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        GroceryStore store = GroceryStore.builder()
                .outletId("store101")
                .build();

        product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .sellingPrice(new BigDecimal("10.50"))
                .availableStock(5)
                .build();

        cart = Cart.builder()
                .cartId("cart101")
                .outlet(store)
                .cartItems(new ArrayList<>(List.of(
                        new CartItem(product, 2)
                )))
                .build();
    }

    @Test
    void shouldPlaceAndSaveOrderThenClearCart() {
        stubCheckout();

        Order order = orderService.placeOrder("user101");

        assertNotNull(order.getOrderId());
        assertFalse(order.getOrderId().isBlank());
        assertEquals("user101", order.getUserId());
        assertEquals("store101", order.getOutletId());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(NOW, order.getCreatedAt());
        assertEquals(new BigDecimal("21.00"), order.getTotalPrice());
        assertEquals(1, order.getItems().size());
        assertEquals(2, order.getItems().getFirst().getQuantity());
        assertNull(order.getItems().getFirst().getSpiceLevel());
        assertNull(order.getItems().getFirst().getUtensilsNeeded());

        assertSame(
                order,
                orderRepository.findById(order.getOrderId()).orElseThrow()
        );
        assertTrue(cart.getCartItems().isEmpty());
        assertEquals(order.getOrderId(), user.getLastOrderId());
    }

    @Test
    void shouldPreserveEachMealLinesCustomizationWhenPlacingOrder() {
        Restaurant restaurant = Restaurant.builder()
                .outletId("restaurant101")
                .build();
        FoodProduct meal = FoodProduct.builder()
                .productId("meal101")
                .productName("Chicken Curry")
                .unitPrice(new BigDecimal("25.00"))
                .availableStock(5)
                .build();
        restaurant.addProduct(meal);

        CartItem mild = new CartItem(meal, 1, meal.getUnitPrice(), SpiceLevel.MILD, false);
        CartItem hot = new CartItem(meal, 2, meal.getUnitPrice(), SpiceLevel.HOT, true);
        cart.setOutlet(restaurant);
        cart.setCartItems(new ArrayList<>(List.of(mild, hot)));
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        when(productService.getProduct("meal101", "restaurant101")).thenReturn(meal);

        Order order = orderService.placeOrder("user101");
        Order saved = orderRepository.findById(order.getOrderId()).orElseThrow();

        assertSame(order, saved);
        assertEquals("restaurant101", saved.getOutletId());
        assertEquals(new BigDecimal("75.00"), saved.getTotalPrice());
        assertEquals(2, saved.getItems().size());

        CartItem first = saved.getItems().getFirst();
        CartItem second = saved.getItems().get(1);
        assertNotSame(mild, first);
        assertNotSame(hot, second);
        assertEquals(mild.getCartItemId(), first.getCartItemId());
        assertEquals(hot.getCartItemId(), second.getCartItemId());
        assertEquals(1, first.getQuantity());
        assertEquals(SpiceLevel.MILD, first.getSpiceLevel());
        assertEquals(Boolean.FALSE, first.getUtensilsNeeded());
        assertEquals(2, second.getQuantity());
        assertEquals(SpiceLevel.HOT, second.getSpiceLevel());
        assertEquals(Boolean.TRUE, second.getUtensilsNeeded());
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void shouldRejectEmptyCartWithoutSavingOrder() {
        cart.getCartItems().clear();
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        assertThrows(
                EmptyCartException.class,
                () -> orderService.placeOrder("user101")
        );

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectInsufficientStockAndPreserveCart() {
        stubCheckout();
        product.setAvailableStock(1);

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder("user101")
        );

        assertEquals("Requested quantity 2 exceeds available stock 1 for product product101",
                exception.getMessage());
        assertEquals(1, cart.getCartItems().size());
        assertEquals(2, cart.getCartItems().getFirst().getQuantity());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldCheckCombinedQuantityAcrossDuplicateProductLines() {
        stubCheckout();

        // Each line individually fits stock, but 2 + 4 exceeds 5.
        cart.getCartItems().add(new CartItem(product, 4));

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder("user101")
        );

        assertEquals(2, cart.getCartItems().size());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldHoldCartLockDuringValidationAndSaving() {
        product = spy(product);
        doAnswer(invocation -> {
            assertTrue(Thread.holdsLock(product), "Stock reads must share the inventory update lock");
            return invocation.callRealMethod();
        }).when(product).getAvailableStock();
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        when(productService.getProduct("product101", "store101"))
                .thenAnswer(invocation -> {
                    assertTrue(Thread.holdsLock(cart));
                    return product;
                });
        doAnswer(invocation -> {
            assertTrue(Thread.holdsLock(cart));
            assertFalse(cart.getCartItems().isEmpty());
            return invocation.callRealMethod();
        }).when(orderRepository).save(any(Order.class));

        Order order = orderService.placeOrder("user101");

        assertTrue(cart.getCartItems().isEmpty());
        assertSame(order, orderRepository.findById(order.getOrderId()).orElseThrow());
    }

    @Test
    void shouldCreateOnlyOneOrderForConcurrentCheckoutsOfSameCart() throws Exception {
        stubCheckout();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var task = (java.util.concurrent.Callable<Optional<Order>>) () -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                try {
                    return Optional.of(orderService.placeOrder("user101"));
                } catch (EmptyCartException exception) {
                    return Optional.empty();
                }
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            long orders = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS))
                    .stream().filter(Optional::isPresent).count();

            assertEquals(1, orders);
            assertTrue(cart.getCartItems().isEmpty());
            verify(orderRepository, times(1)).save(any(Order.class));
        } finally {
            start.countDown();
        }
    }

    @Test
    void shouldRejectChangedPriceAndPreserveCartPrice() {
        stubCheckout();

        // Cart already captured 10.50.
        product.setSellingPrice(new BigDecimal("11.50"));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder("user101")
        );

        assertEquals(1, cart.getCartItems().size());
        assertEquals(
                new BigDecimal("10.50"),
                cart.getCartItems().getFirst().getUnitPrice()
        );
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldPreserveCartWhenSavingOrderFails() {
        stubCheckout();
        user.setLastOrderId("previous-order");
        doThrow(new IllegalStateException("Storage unavailable"))
                .when(orderRepository).save(any(Order.class));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder("user101")
        );

        assertEquals(1, cart.getCartItems().size());
        assertEquals(2, cart.getCartItems().getFirst().getQuantity());
        assertEquals("previous-order", user.getLastOrderId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRemoveNewOrderAndPreserveCartAndHistoryIfHistorySaveFails() {
        stubCheckout();
        user.setLastOrderId("previous-order");
        doThrow(new IllegalStateException("User storage unavailable"))
                .when(userRepository).save(user);

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder("user101"));

        var captured = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captured.capture());
        String newOrderId = captured.getValue().getOrderId();
        verify(orderRepository).deleteById(newOrderId);
        assertTrue(orderRepository.findById(newOrderId).isEmpty());
        assertEquals("previous-order", user.getLastOrderId());
        assertEquals(1, cart.getCartItems().size());
        assertEquals(2, cart.getCartItems().getFirst().getQuantity());
    }

    @Test
    void shouldReturnExistingOrder() {
        Order order = existingOrder();

        assertSame(order, orderService.getOrder("order101"));
    }

    @Test
    void shouldRejectUnknownOrder() {
        assertThrows(
                OrderDoesNotExistException.class,
                () -> orderService.getOrder("missing-order")
        );
    }

    @Test
    void shouldChangeAndSaveValidStatus() {
        Order order = existingOrder();
        clearInvocations(orderRepository);

        Order result = orderService.changeStatus(
                "order101", OrderStatus.CONFIRMED
        );

        assertSame(order, result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldRejectInvalidStatusWithoutSavingOrChangingOrder() {
        Order order = existingOrder();
        clearInvocations(orderRepository);

        assertThrows(
                IllegalStateException.class,
                () -> orderService.changeStatus(
                        "order101", OrderStatus.DELIVERED
                )
        );

        assertEquals(OrderStatus.PLACED, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    private void stubCheckout() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        when(productService.getProduct("product101", "store101"))
                .thenReturn(product);
    }

    private Order existingOrder() {
        Order order = new Order(
                "order101",
                "user101",
                "store101",
                List.of(new CartItem(product, 2)),
                NOW
        );

        orderRepository.save(order);
        return order;
    }
}
