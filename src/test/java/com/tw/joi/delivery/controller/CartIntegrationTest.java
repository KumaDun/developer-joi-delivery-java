package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.seedData.SeedData;
import com.tw.joi.delivery.repository.CartRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    private Cart cart;
    private List<CartItem> originalItems;
    private Outlet<?> originalOutlet;

    @BeforeEach
    void setUp() {
        cart = cartRepository.getOrCreateForUser(SeedData.user101);
        originalItems = cart.getCartItems();
        originalOutlet = cart.getOutlet();
        cart.setCartItems(new ArrayList<>());
        cart.setOutlet(null);
    }

    @AfterEach
    void restoreCart() {
        cart.setCartItems(originalItems);
        cart.setOutlet(originalOutlet);
    }

    @Test
    void shouldAddCustomizedMealAndViewSameSelections() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "user101",
                              "outletId": "restaurant101",
                              "productId": "meal101",
                              "quantity": 2,
                              "spiceLevel": "MEDIUM",
                              "utensilsNeeded": true
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("meal101"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(50.0))
                .andExpect(jsonPath("$.spiceLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.utensilsNeeded").value(true));

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view").param("userId", "user101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outlet.outletId").value("restaurant101"))
                .andExpect(jsonPath("$.cartItems.length()").value(1))
                .andExpect(jsonPath("$.cartItems[0].spiceLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.cartItems[0].utensilsNeeded").value(true));

        assertEquals(20, SeedData.restaurant101.findProduct("meal101").orElseThrow().getAvailableStock());
    }

    @Test
    void shouldDefaultMealChoicesWhenOmitted() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("meal101", "restaurant101")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spiceLevel").value("NONE"))
                .andExpect(jsonPath("$.utensilsNeeded").value(false));

        assertSame(SeedData.restaurant101, cart.getOutlet());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"spiceLevel\":\"HOT\"",
            "\"utensilsNeeded\":true",
            "\"utensilsNeeded\":false"
    })
    void shouldRejectGroceryCustomizationWithoutChangingCart(String option) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "user101",
                              "outletId": "store101",
                              "productId": "product101",
                              "quantity": 1,
                              %s
                            }
                            """.formatted(option)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Spice level and utensils are only available for restaurant meals"));

        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
    }

    @ParameterizedTest
    @CsvSource({
            "product101, store101, meal101, restaurant101",
            "meal101, restaurant101, product101, store101"
    })
    void shouldRejectMixingRestaurantAndGroceryOutlets(
            String firstProduct, String firstOutlet, String secondProduct, String secondOutlet)
            throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(firstProduct, firstOutlet)))
                .andExpect(status().isOk());

        CartItem originalItem = cart.getCartItems().getFirst();
        Outlet<?> outlet = cart.getOutlet();

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(secondProduct, secondOutlet)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart can only contain products from one outlet"));

        assertEquals(1, cart.getCartItems().size());
        assertSame(originalItem, cart.getCartItems().getFirst());
        assertSame(outlet, cart.getOutlet());
    }

    @Test
    void shouldLazilyExposeCartForSecondUserWithCorrectOwner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view").param("userId", "user102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").isNotEmpty())
                .andExpect(jsonPath("$.user.userId").value("user102"))
                .andExpect(jsonPath("$.cartItems").isEmpty())
                .andExpect(jsonPath("$.outlet").isEmpty());
    }

    @Test
    void shouldPreserveMealChoicesThroughCheckoutAndCartReuse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"user101","outletId":"restaurant101","productId":"meal101",
                             "quantity":1,"spiceLevel":"MILD","utensilsNeeded":false}
                            """))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"user101","outletId":"restaurant101","productId":"meal101",
                             "quantity":2,"spiceLevel":"HOT","utensilsNeeded":true}
                            """))
                .andExpect(status().isOk());

        ResultActions placedOrder = mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/orders/")));
        expectCustomizedMealOrder(placedOrder);
        String location = placedOrder.andReturn().getResponse().getHeader("Location");

        expectCustomizedMealOrder(mockMvc.perform(MockMvcRequestBuilders.get(location))
                .andExpect(status().isOk()));
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view").param("userId", "user101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems").isEmpty());

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("product101", "store101")))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view").param("userId", "user101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outlet.outletId").value("store101"))
                .andExpect(jsonPath("$.cartItems[0].productId").value("product101"));
        expectCustomizedMealOrder(mockMvc.perform(MockMvcRequestBuilders.get(location))
                .andExpect(status().isOk()));
    }

    @Test
    void shouldCreateAndRetrieveGroceryOrderWithoutMealOptions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("product101", "store101")))
                .andExpect(status().isOk());

        String location = mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/orders/")))
                .andExpect(jsonPath("$.outletId").value("store101"))
                .andExpect(jsonPath("$.totalPrice").value(10.5))
                .andExpect(jsonPath("$.items[0].productId").value("product101"))
                .andExpect(jsonPath("$.items[0].spiceLevel").doesNotExist())
                .andExpect(jsonPath("$.items[0].utensilsNeeded").doesNotExist())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(MockMvcRequestBuilders.get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalPrice").value(10.5))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10.5))
                .andExpect(jsonPath("$.items[0].spiceLevel").doesNotExist())
                .andExpect(jsonPath("$.items[0].utensilsNeeded").doesNotExist());
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view").param("userId", "user101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems").isEmpty());
    }

    @Test
    void shouldReturnStockConflictForCombinedCartQuantityWithoutChangingItems() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("meal101", "restaurant101", 10)))
                .andExpect(status().isOk());
        CartItem originalItem = cart.getCartItems().getFirst();

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("meal101", "restaurant101", 11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Requested quantity 21 exceeds available stock 20 for product meal101"));

        assertEquals(List.of(originalItem), cart.getCartItems());
        assertEquals(10, originalItem.getQuantity());
        assertSame(SeedData.restaurant101, cart.getOutlet());
        assertEquals(20, SeedData.restaurant101.findProduct("meal101").orElseThrow().getAvailableStock());
    }

    @Test
    void shouldReturnStockConflictAtCheckoutAndPreserveCart() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("meal101", "restaurant101", 2)))
                .andExpect(status().isOk());
        CartItem originalItem = cart.getCartItems().getFirst();
        FoodProduct meal = SeedData.restaurant101.findProduct("meal101").orElseThrow();
        int originalStock = meal.getAvailableStock();
        try {
            meal.setAvailableStock(1);

            mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("Requested quantity 2 exceeds available stock 1 for product meal101"));

            assertEquals(List.of(originalItem), cart.getCartItems());
            assertSame(SeedData.restaurant101, cart.getOutlet());
        } finally {
            meal.setAvailableStock(originalStock);
        }
    }

    @Test
    void shouldReturnConflictWhenCheckingOutAnEmptyCart() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot place an order without a nonempty cart"));

        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getOutlet());
    }

    private void expectCustomizedMealOrder(ResultActions response) throws Exception {
        response.andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.outletId").value("restaurant101"))
                .andExpect(jsonPath("$.totalPrice").value(75.0))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productId").value("meal101"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].unitPrice").value(25.0))
                .andExpect(jsonPath("$.items[0].totalPrice").value(25.0))
                .andExpect(jsonPath("$.items[0].spiceLevel").value("MILD"))
                .andExpect(jsonPath("$.items[0].utensilsNeeded").value(false))
                .andExpect(jsonPath("$.items[1].productId").value("meal101"))
                .andExpect(jsonPath("$.items[1].quantity").value(2))
                .andExpect(jsonPath("$.items[1].unitPrice").value(25.0))
                .andExpect(jsonPath("$.items[1].totalPrice").value(50.0))
                .andExpect(jsonPath("$.items[1].spiceLevel").value("HOT"))
                .andExpect(jsonPath("$.items[1].utensilsNeeded").value(true));
    }

    private String request(String productId, String outletId) {
        return request(productId, outletId, 1);
    }

    private String request(String productId, String outletId, int quantity) {
        return """
                {"userId":"user101","productId":"%s","outletId":"%s","quantity":%d}
                """.formatted(productId, outletId, quantity);
    }
}
