package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.repository.CartRepository;
import com.tw.joi.delivery.repository.OrderRepository;
import com.tw.joi.delivery.repository.StoreRepository;
import com.tw.joi.delivery.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderConflictIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private CartRepository cartRepository;
    @MockitoSpyBean
    private OrderRepository orderRepository;

    @Test
    void shouldReturnPriceChangeMessageWithoutSavingOrderOrChangingCartAndHistory() throws Exception {
        Fixture fixture = fixture();
        addMeal(fixture, 1);
        checkout(fixture.user().getUserId());
        String previousOrderId = fixture.user().getLastOrderId();
        assertNotNull(previousOrderId);

        addMeal(fixture, 2);
        Cart cart = cartRepository.getOrCreateForUser(fixture.user());
        CartItem originalItem = cart.getCartItems().getFirst();
        synchronized (fixture.meal()) {
            fixture.meal().setUnitPrice(new BigDecimal("11.50"));
        }
        clearInvocations(orderRepository);

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(fixture.user().getUserId())))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.message").value(
                        "Order not placed: price for product " + fixture.meal().getProductId()
                                + " changed from 10.50 to 11.50."))
                .andExpect(jsonPath("$.orderId").doesNotExist());

        verify(orderRepository, never()).save(any(Order.class));
        assertEquals(1, cart.getCartItems().size());
        assertSame(originalItem, cart.getCartItems().getFirst());
        assertSame(fixture.restaurant(), cart.getOutlet());
        assertEquals(2, originalItem.getQuantity());
        assertEquals(new BigDecimal("10.50"), originalItem.getUnitPrice());
        assertEquals(previousOrderId, fixture.user().getLastOrderId());
        assertTrue(orderRepository.findById(previousOrderId).isPresent());

        mockMvc.perform(get("/users/{userId}/last-purchase", fixture.user().getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(previousOrderId));
    }

    @Test
    void shouldCreateOrderAndRejectInvalidTransitionWithoutChangingSavedStatus() throws Exception {
        Fixture fixture = fixture();
        addMeal(fixture, 1);
        String location = checkout(fixture.user().getUserId());
        String orderId = fixture.user().getLastOrderId();
        assertNotNull(orderId);
        assertEquals("/orders/" + orderId, location);
        assertTrue(cartRepository.getOrCreateForUser(fixture.user()).getCartItems().isEmpty());
        clearInvocations(orderRepository);

        mockMvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot transition order from PLACED to DELIVERED"));

        verify(orderRepository, never()).save(any(Order.class));
        assertEquals(OrderStatus.PLACED, orderRepository.findById(orderId).orElseThrow().getStatus());
        assertEquals(orderId, fixture.user().getLastOrderId());
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    private Fixture fixture() {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder().userId("order-user-" + suffix).build();
        userRepository.save(user);
        Restaurant restaurant = Restaurant.builder()
                .outletId("order-restaurant-" + suffix).name("Order Test Kitchen").build();
        FoodProduct meal = FoodProduct.builder()
                .productId("order-meal-" + suffix).productName("Curry")
                .unitPrice(new BigDecimal("10.50")).availableStock(10).build();
        restaurant.addProduct(meal);
        storeRepository.save(restaurant);
        return new Fixture(user, restaurant, meal);
    }

    private void addMeal(Fixture fixture, int quantity) throws Exception {
        mockMvc.perform(post("/cart/product").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","outletId":"%s","productId":"%s",
                             "quantity":%d,"spiceLevel":"HOT","utensilsNeeded":true}
                            """.formatted(fixture.user().getUserId(), fixture.restaurant().getOutletId(),
                                fixture.meal().getProductId(), quantity)))
                .andExpect(status().isOk());
    }

    private String checkout(String userId) throws Exception {
        return mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/orders/")))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn().getResponse().getHeader("Location");
    }

    private record Fixture(User user, Restaurant restaurant, FoodProduct meal) {
    }
}
