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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LastPurchaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnNoContentForUserWithoutOrders() throws Exception {
        User user = createUser();

        mockMvc.perform(get("/users/{userId}/last-purchase", user.getUserId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRememberLatestPlacedOutletAndIgnoreFailedCheckout() throws Exception {
        User user = createUser();
        String userId = user.getUserId();
        addProduct(userId, "product101", "store101");
        JsonNode firstOrder = checkout(userId);
        expectPurchase(userId, firstOrder, "Fresh Picks");

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(userId)))
                .andExpect(status().isConflict());
        expectPurchase(userId, firstOrder, "Fresh Picks");

        addProduct(userId, "meal101", "restaurant101");
        JsonNode secondOrder = checkout(userId);
        expectPurchase(userId, secondOrder, "Spice Kitchen");

        mockMvc.perform(get("/cart/view").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.lastOrderId").doesNotExist());
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {
        mockMvc.perform(get("/users/{userId}/last-purchase", "missing-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private User createUser() {
        User user = User.builder().userId("history-" + UUID.randomUUID()).firstName("History").build();
        userRepository.save(user);
        return user;
    }

    private void addProduct(String userId, String productId, String outletId) throws Exception {
        mockMvc.perform(post("/cart/product").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","productId":"%s","outletId":"%s","quantity":1}
                            """.formatted(userId, productId, outletId)))
                .andExpect(status().isOk());
    }

    private JsonNode checkout(String userId) throws Exception {
        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private void expectPurchase(String userId, JsonNode order, String outletName) throws Exception {
        mockMvc.perform(get("/users/{userId}/last-purchase", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.get("orderId").asText()))
                .andExpect(jsonPath("$.outletId").value(order.get("outletId").asText()))
                .andExpect(jsonPath("$.outletName").value(outletName))
                .andExpect(jsonPath("$.orderedAt").value(order.get("createdAt").asText()));
    }
}
