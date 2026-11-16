package com.tw.joi.delivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.Restaurant;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.repository.StoreRepository;
import com.tw.joi.delivery.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StrictRequestBindingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoreRepository storeRepository;

    private String userId;
    private String outletId;
    private FoodProduct meal;

    @BeforeEach
    void createIndependentFixtures() {
        String suffix = UUID.randomUUID().toString();
        userId = "strict-user-" + suffix;
        userRepository.save(User.builder().userId(userId).firstName("Strict").build());
        outletId = "strict-restaurant-" + suffix;
        Restaurant restaurant = Restaurant.builder().outletId(outletId).name("Strict Kitchen").build();
        meal = FoodProduct.builder()
                .productId("strict-meal-" + suffix)
                .productName("Curry")
                .unitPrice(new BigDecimal("25.00"))
                .availableStock(10)
                .build();
        restaurant.addProduct(meal);
        storeRepository.save(restaurant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.9", "-0.5", "1.9", "1.0"})
    void shouldRejectFractionalAddQuantityWithoutChangingCart(String quantity) throws Exception {
        JsonNode before = viewCart();

        mockMvc.perform(post("/cart/product").contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest(quantity, "\"MILD\"")))
                .andExpect(status().isBadRequest());

        assertEquals(before, viewCart());
        assertEquals(10, meal.getAvailableStock());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.9", "-0.5", "2.9", "1.0"})
    void shouldRejectFractionalUpdateWithoutRemovingOrChangingLine(String quantity) throws Exception {
        String itemId = addMeal().get("cartItemId").asText();
        JsonNode before = viewCart();

        mockMvc.perform(patch("/cart/items/{cartItemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"quantity\":%s}".formatted(userId, quantity)))
                .andExpect(status().isBadRequest());

        assertEquals(before, viewCart());
    }

    @ParameterizedTest
    @CsvSource({
            "PUT, availableStock, 0.9",
            "PUT, availableStock, -0.5",
            "PUT, availableStock, 1.0",
            "POST, delta, 1.9",
            "POST, delta, -1.9"
    })
    void shouldRejectFractionalStockCountsWithoutChangingInventory(
            String method, String field, String value) throws Exception {
        String stockUrl = "/inventory/{outletId}/products/{productId}/stock";
        MockHttpServletRequestBuilder request = method.equals("PUT")
                ? put(stockUrl, outletId, meal.getProductId())
                : post("/inventory/{outletId}/products/{productId}/stock-adjustments",
                        outletId, meal.getProductId());

        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                        .content("{\"%s\":%s}".formatted(field, value)))
                .andExpect(status().isBadRequest());

        synchronized (meal) {
            assertEquals(10, meal.getAvailableStock());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "\"1\""})
    void shouldRejectNumericSpiceOrdinalWithoutAddingItem(String spice) throws Exception {
        JsonNode before = viewCart();

        mockMvc.perform(post("/cart/product").contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest("2", spice)))
                .andExpect(status().isBadRequest());

        assertEquals(before, viewCart());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "\"2\""})
    void shouldRejectNumericOrderStatusOrdinalWithoutChangingOrder(String status) throws Exception {
        addMeal();
        JsonNode order = checkout();
        String orderId = order.get("orderId").asText();

        mockMvc.perform(patch("/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":%s}".formatted(status)))
                .andExpect(status().isBadRequest());

        String response = mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(order, objectMapper.readTree(response));
    }

    @Test
    void shouldContinueAcceptingIntegerCountsAndNamedEnums() throws Exception {
        mockMvc.perform(put("/inventory/{outletId}/products/{productId}/stock", outletId, meal.getProductId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(12));
        mockMvc.perform(post("/inventory/{outletId}/products/{productId}/stock-adjustments",
                        outletId, meal.getProductId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(10));
        String itemId = addMeal().get("cartItemId").asText();
        mockMvc.perform(patch("/cart/items/{cartItemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"quantity\":3}".formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems[0].quantity").value(3))
                .andExpect(jsonPath("$.cartItems[0].totalPrice").value(75.0));
        JsonNode order = checkout();
        assertEquals("MILD", order.get("items").get(0).get("spiceLevel").asText());

        mockMvc.perform(patch("/orders/{orderId}/status", order.get("orderId").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    private String addRequest(String quantity, String spice) {
        return """
                {"userId":"%s","outletId":"%s","productId":"%s",
                 "quantity":%s,"spiceLevel":%s,"utensilsNeeded":false}
                """.formatted(userId, outletId, meal.getProductId(), quantity, spice);
    }

    private JsonNode addMeal() throws Exception {
        String response = mockMvc.perform(post("/cart/product").contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest("2", "\"MILD\"")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode viewCart() throws Exception {
        String response = mockMvc.perform(get("/cart/view").param("userId", userId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode checkout() throws Exception {
        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
