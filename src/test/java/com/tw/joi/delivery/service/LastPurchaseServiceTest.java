package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryOrderRepository;
import com.tw.joi.delivery.repository.InMemoryOutletRepository;
import com.tw.joi.delivery.repository.InMemoryUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LastPurchaseServiceTest {

    private static final Instant ORDERED_AT = Instant.parse("2026-11-16T10:00:00Z");
    private User user;
    private UserService userService;
    private InMemoryOrderRepository orders;
    private GroceryStore store;
    private LastPurchaseService service;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user101").build();
        userService = new UserService(new InMemoryUserRepository(List.of(user)));
        orders = new InMemoryOrderRepository();
        store = GroceryStore.builder().outletId("store101").name("Fresh Picks").build();
        service = new LastPurchaseService(userService, orders, new InMemoryOutletRepository(List.of(store)));
    }

    @Test
    void shouldReturnNoPurchaseForUserWithoutOrders() {
        assertTrue(service.getLastPurchase("user101").isEmpty());
    }

    @Test
    void shouldReturnOrderMetadataWithCurrentOutletName() {
        orders.save(order("order101", "user101"));
        userService.recordLastOrder("user101", "order101");

        var response = service.getLastPurchase("user101").orElseThrow();

        assertEquals("order101", response.orderId());
        assertEquals("store101", response.outletId());
        assertEquals("Fresh Picks", response.outletName());
        assertEquals(ORDERED_AT, response.orderedAt());
        store.setName("Renamed Store");
        assertEquals("Fresh Picks", response.outletName());
        assertEquals("Renamed Store", service.getLastPurchase("user101").orElseThrow().outletName());
    }

    @Test
    void shouldRejectUnknownUser() {
        assertThrows(UserDoesNotExistException.class, () -> service.getLastPurchase("missing"));
    }

    @Test
    void shouldRejectPointerToMissingOrder() {
        user.setLastOrderId("missing-order");

        assertThrows(IllegalStateException.class, () -> service.getLastPurchase("user101"));
    }

    @Test
    void shouldRejectPointerToAnotherUsersOrder() {
        orders.save(order("other-order", "user102"));
        user.setLastOrderId("other-order");

        assertThrows(IllegalStateException.class, () -> service.getLastPurchase("user101"));
    }

    @Test
    void shouldPreserveOrderAndOutletIdWhenOutletIsUnavailable() {
        orders.save(order("order101", "user101"));
        userService.recordLastOrder("user101", "order101");
        var serviceWithoutOutlets = new LastPurchaseService(
                userService, orders, new InMemoryOutletRepository(List.of()));

        var response = serviceWithoutOutlets.getLastPurchase("user101").orElseThrow();

        assertEquals("order101", response.orderId());
        assertEquals("store101", response.outletId());
        assertNull(response.outletName());
    }

    @Test
    void shouldKeepLastPlacedOrderWhenItIsSubsequentlyCancelled() {
        Order order = order("order101", "user101");
        orders.save(order);
        userService.recordLastOrder("user101", "order101");
        order.transitionTo(OrderStatus.CANCELLED);

        assertEquals("order101", service.getLastPurchase("user101").orElseThrow().orderId());
    }

    private Order order(String orderId, String userId) {
        GroceryProduct product = GroceryProduct.builder()
                .productId("product101").productName("Bread").sellingPrice(BigDecimal.ONE).build();
        return new Order(orderId, userId, "store101", List.of(new CartItem(product, 1)), ORDERED_AT);
    }
}
