package com.tw.joi.delivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CartQuantityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldUpdateOnlySelectedCustomizationLineAndKeepItsId() throws Exception {
        String userId = createUser();
        String mildId = addMeal(userId, 1, "MILD", false).get("cartItemId").asText();
        String hotId = addMeal(userId, 2, "HOT", true).get("cartItemId").asText();
        assertNotEquals(mildId, hotId);
        assertDoesNotThrow(() -> UUID.fromString(mildId));
        assertDoesNotThrow(() -> UUID.fromString(hotId));

        JsonNode increased = update(userId, mildId, 4);
        assertMeal(increased.get("cartItems").get(0), mildId, 4, "MILD", false);
        assertMeal(increased.get("cartItems").get(1), hotId, 2, "HOT", true);

        JsonNode decreased = update(userId, mildId, 2);
        assertMeal(decreased.get("cartItems").get(0), mildId, 2, "MILD", false);
        assertMeal(decreased.get("cartItems").get(1), hotId, 2, "HOT", true);
        assertEquals(decreased, view(userId));
    }

    @Test
    void shouldRemoveLinesAndClearOutletWhenLastQuantityBecomesZero() throws Exception {
        String userId = createUser();
        String mildId = addMeal(userId, 1, "MILD", false).get("cartItemId").asText();
        String hotId = addMeal(userId, 2, "HOT", true).get("cartItemId").asText();

        JsonNode remaining = update(userId, mildId, 0);
        assertEquals(1, remaining.get("cartItems").size());
        assertMeal(remaining.get("cartItems").get(0), hotId, 2, "HOT", true);
        assertEquals("restaurant101", remaining.get("outlet").get("outletId").asText());

        JsonNode empty = update(userId, hotId, 0);
        assertTrue(empty.get("cartItems").isEmpty());
        assertTrue(empty.get("outlet").isNull());
        assertEquals(empty, view(userId));
    }

    @Test
    void shouldRejectAggregateStockOverflowWithoutChangingEitherLine() throws Exception {
        String userId = createUser();
        String mildId = addMeal(userId, 10, "MILD", false).get("cartItemId").asText();
        addMeal(userId, 10, "HOT", true);
        JsonNode before = view(userId);

        mockMvc.perform(patch("/cart/items/{cartItemId}", mildId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityRequest(userId, 11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Requested quantity 21 exceeds available stock 20 for product meal101"));

        assertEquals(before, view(userId));
    }

    @Test
    void shouldNotUpdateLineBelongingToAnotherUsersCart() throws Exception {
        String ownerId = createUser();
        String otherId = createUser();
        String itemId = addMeal(ownerId, 2, "MILD", false).get("cartItemId").asText();
        JsonNode ownersCart = view(ownerId);

        mockMvc.perform(patch("/cart/items/{cartItemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityRequest(otherId, 3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cart item: " + itemId + " does not exist"));

        assertEquals(ownersCart, view(ownerId));
        assertTrue(view(otherId).get("cartItems").isEmpty());
    }

    @Test
    void shouldKeepUpdatedIdsAndOptionsInOrderWhenCartIsReused() throws Exception {
        String userId = createUser();
        String mildId = addMeal(userId, 1, "MILD", false).get("cartItemId").asText();
        String hotId = addMeal(userId, 1, "HOT", true).get("cartItemId").asText();
        update(userId, mildId, 3);

        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode order = objectMapper.readTree(response);
        assertMeal(order.get("items").get(0), mildId, 3, "MILD", false);
        assertMeal(order.get("items").get(1), hotId, 1, "HOT", true);
        assertEquals(100.0, order.get("totalPrice").asDouble());
        assertTrue(view(userId).get("cartItems").isEmpty());

        String newItemResponse = mockMvc.perform(post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","outletId":"store101","productId":"product101","quantity":1}
                            """.formatted(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String groceryId = objectMapper.readTree(newItemResponse).get("cartItemId").asText();
        assertNotEquals(mildId, groceryId);
        assertNotEquals(hotId, groceryId);
        JsonNode reusedCart = update(userId, groceryId, 2);
        assertEquals("store101", reusedCart.get("outlet").get("outletId").asText());
        assertEquals(21.0, reusedCart.get("cartItems").get(0).get("totalPrice").asDouble());
        assertFalse(reusedCart.get("cartItems").get(0).has("spiceLevel"));
        assertFalse(reusedCart.get("cartItems").get(0).has("utensilsNeeded"));

        String savedResponse = mockMvc.perform(get("/orders/{orderId}", order.get("orderId").asText()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(order, objectMapper.readTree(savedResponse));
    }

    private String createUser() {
        String userId = "cart-quantity-" + UUID.randomUUID();
        userRepository.save(User.builder().userId(userId).firstName("Quantity").build());
        return userId;
    }

    private JsonNode addMeal(String userId, int quantity, String spiceLevel, boolean utensils) throws Exception {
        String response = mockMvc.perform(post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","outletId":"restaurant101","productId":"meal101",
                             "quantity":%d,"spiceLevel":"%s","utensilsNeeded":%s}
                            """.formatted(userId, quantity, spiceLevel, utensils)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode update(String userId, String cartItemId, int quantity) throws Exception {
        String response = mockMvc.perform(patch("/cart/items/{cartItemId}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityRequest(userId, quantity)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode view(String userId) throws Exception {
        String response = mockMvc.perform(get("/cart/view").param("userId", userId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String quantityRequest(String userId, int quantity) {
        return "{\"userId\":\"%s\",\"quantity\":%d}".formatted(userId, quantity);
    }

    private void assertMeal(JsonNode item, String cartItemId, int quantity, String spiceLevel, boolean utensils) {
        assertEquals(cartItemId, item.get("cartItemId").asText());
        assertEquals("meal101", item.get("productId").asText());
        assertEquals(quantity, item.get("quantity").asInt());
        assertEquals(25.0, item.get("unitPrice").asDouble());
        assertEquals(25.0 * quantity, item.get("totalPrice").asDouble());
        assertEquals(spiceLevel, item.get("spiceLevel").asText());
        assertEquals(utensils, item.get("utensilsNeeded").asBoolean());
    }
}
