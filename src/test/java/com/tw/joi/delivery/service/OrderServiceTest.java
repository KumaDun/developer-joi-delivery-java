package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.*;
import com.tw.joi.delivery.exception.OrderDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryOrderRepository;
import com.tw.joi.delivery.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

    private GroceryProduct product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        productService = mock(ProductService.class);
        orderRepository = spy(new InMemoryOrderRepository());

        orderService = new OrderService(
                cartService,
                productService,
                orderRepository,
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

        assertSame(
                order,
                orderRepository.findById(order.getOrderId()).orElseThrow()
        );
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void shouldRejectEmptyCartWithoutSavingOrder() {
        cart.getCartItems().clear();
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder("user101")
        );

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectInsufficientStockAndPreserveCart() {
        stubCheckout();
        product.setAvailableStock(1);

        assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder("user101")
        );

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
                IllegalStateException.class,
                () -> orderService.placeOrder("user101")
        );

        assertEquals(2, cart.getCartItems().size());
        verify(orderRepository, never()).save(any(Order.class));
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
        doThrow(new IllegalStateException("Storage unavailable"))
                .when(orderRepository).save(any(Order.class));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder("user101")
        );

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