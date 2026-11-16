package com.tw.joi.delivery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.SpiceLevel;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.exception.CartItemDoesNotExistException;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.service.CartService;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.List;

@WebMvcTest(CartController.class)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldRejectMissingUserId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldRejectBlankUserId(String userId) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                        .param("userId", userId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        String userId = "missing";

        when(cartService.getCartSnapshotForUser(userId))
                .thenThrow(new UserDoesNotExistException(userId));

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                        .param("userId", userId))
                .andExpect(status().isNotFound());

        verify(cartService).getCartSnapshotForUser(userId);
    }

    @Test
    void shouldAddTheRequestedProductToTheCart() throws Exception {
        AddProductRequest request = new AddProductRequest();
        request.setUserId("user101");
        request.setOutletId("store101");
        request.setProductId("product101");
        request.setQuantity(2);

        GroceryProduct product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .sellingPrice(new BigDecimal("10.50"))
                .build();

        CartItem item = new CartItem(product, 2);

        when(cartService.addProductToCartForUser(any(AddProductRequest.class)))
                .thenReturn(item);

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItemId")
                        .value(item.getCartItemId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.productId")
                        .value("product101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.productName")
                        .value("Wheat Bread"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.quantity")
                        .value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitPrice")
                        .value(10.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice")
                        .value(21.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.spiceLevel")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.utensilsNeeded")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cart")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.product")
                        .doesNotExist());

        ArgumentCaptor<AddProductRequest> captor =
                ArgumentCaptor.forClass(AddProductRequest.class);

        verify(cartService).addProductToCartForUser(captor.capture());

        AddProductRequest captured = captor.getValue();

        assertEquals("user101", captured.getUserId());
        assertEquals("store101", captured.getOutletId());
        assertEquals("product101", captured.getProductId());
        assertEquals(2, captured.getQuantity());
    }

    @Test
    void shouldBindAndReturnRestaurantCustomization() throws Exception {
        FoodProduct meal = FoodProduct.builder()
                .productId("meal101")
                .productName("Chicken Curry")
                .unitPrice(new BigDecimal("25.00"))
                .build();
        CartItem item = new CartItem(meal, 2, meal.getUnitPrice(), SpiceLevel.HOT, true);
        when(cartService.addProductToCartForUser(any(AddProductRequest.class)))
                .thenReturn(item);

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "user101",
                              "outletId": "restaurant101",
                              "productId": "meal101",
                              "quantity": 2,
                              "spiceLevel": "HOT",
                              "utensilsNeeded": true
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.productId").value("meal101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.quantity").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice").value(50.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.spiceLevel").value("HOT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.utensilsNeeded").value(true));

        ArgumentCaptor<AddProductRequest> captor = ArgumentCaptor.forClass(AddProductRequest.class);
        verify(cartService).addProductToCartForUser(captor.capture());
        assertEquals(SpiceLevel.HOT, captor.getValue().getSpiceLevel());
        assertEquals(Boolean.TRUE, captor.getValue().getUtensilsNeeded());
    }

    @Test
    void shouldRejectUnknownSpiceLevelBeforeCallingService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "user101",
                              "outletId": "restaurant101",
                              "productId": "meal101",
                              "quantity": 1,
                              "spiceLevel": "UNKNOWN"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void shouldReturnTheCart() throws Exception {
        String url = "/cart/view?userId={userId}";
        String userId="user101";
        Cart cart = Cart.builder().cartId("cart101").build();

        when(cartService.getCartSnapshotForUser(userId))
                .thenReturn(cart);

        mockMvc.perform(MockMvcRequestBuilders.get(url,"user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.cartId", Is.is("cart101")));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(cartService.addProductToCartForUser(any(AddProductRequest.class)))
                .thenThrow(new ProductDoesNotExistException("missing"));

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "userId": "user101",
                    "outletId": "store101",
                    "productId": "missing",
                    "quantity": 1
                }
                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnCartItemsWithoutFollowingBackReferences() throws Exception {
        GroceryProduct product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .sellingPrice(new BigDecimal("10.50"))
                .build();

        CartItem item = new CartItem(product, 2);

        GroceryStore store = GroceryStore.builder()
                .outletId("store101")
                .name("Fresh Picks")
                .build();
        store.addProduct(product);

        User user = User.builder().userId("user101").build();
        Cart cart = Cart.builder()
                .cartId("cart101")
                .outlet(store)
                .cartItems(List.of(item))
                .user(user)
                .build();
        user.setCart(cart);

        when(cartService.getCartSnapshotForUser("user101"))
                .thenReturn(cart);

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                        .param("userId", "user101"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartId")
                        .value("cart101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].productId")
                        .value("product101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].cartItemId")
                        .value(item.getCartItemId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].quantity")
                        .value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].unitPrice")
                        .value(10.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].totalPrice")
                        .value(21.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.outlet.inventory[0].productId")
                        .value("product101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.outlet.inventory[0].outlet")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.outlet.inventory[0].store")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.userId")
                        .value("user101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.cart")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.products")
                        .doesNotExist());

        verify(cartService).getCartSnapshotForUser("user101");
    }

    @Test
    void shouldBindQuantityUpdateAndReturnCartSnapshot() throws Exception {
        FoodProduct meal = FoodProduct.builder()
                .productId("meal101").productName("Chicken Curry")
                .unitPrice(new BigDecimal("25.00")).build();
        CartItem item = new CartItem(meal, 3, meal.getUnitPrice(), SpiceLevel.HOT, true);
        Cart snapshot = Cart.builder().cartId("cart101").cartItems(List.of(item)).build();
        when(cartService.updateCartItemQuantity("user101", item.getCartItemId(), 3))
                .thenReturn(snapshot);

        mockMvc.perform(MockMvcRequestBuilders.patch("/cart/items/{cartItemId}", item.getCartItemId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\",\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartId").value("cart101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].cartItemId")
                        .value(item.getCartItemId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].quantity").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].totalPrice").value(75.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].spiceLevel").value("HOT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems[0].utensilsNeeded").value(true));

        verify(cartService).updateCartItemQuantity("user101", item.getCartItemId(), 3);
    }

    @Test
    void shouldAllowZeroQuantityToReachRemovalLogic() throws Exception {
        when(cartService.updateCartItemQuantity("user101", "item101", 0))
                .thenReturn(Cart.builder().cartId("cart101").build());

        mockMvc.perform(MockMvcRequestBuilders.patch("/cart/items/item101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\",\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems").isEmpty());

        verify(cartService).updateCartItemQuantity("user101", "item101", 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"userId\":\"user101\",\"quantity\":-1}",
            "{\"userId\":\"user101\"}",
            "{\"userId\":\"user101\",\"quantity\":null}",
            "{\"quantity\":1}",
            "{\"userId\":\"\",\"quantity\":1}",
            "{\"userId\":\" \",\"quantity\":1}"
    })
    void shouldRejectInvalidQuantityUpdateWithoutCallingService(String body) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/cart/items/item101")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownItem() throws Exception {
        when(cartService.updateCartItemQuantity("user101", "missing", 2))
                .thenThrow(new CartItemDoesNotExistException("missing"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/cart/items/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\",\"quantity\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Cart item: missing does not exist"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownUsersCart() throws Exception {
        when(cartService.updateCartItemQuantity("missing", "item101", 2))
                .thenThrow(new UserDoesNotExistException("missing"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/cart/items/item101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"missing\",\"quantity\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenUpdatedQuantityExceedsStock() throws Exception {
        when(cartService.updateCartItemQuantity("user101", "item101", 20))
                .thenThrow(new InsufficientStockException("meal101", 21, 20));

        mockMvc.perform(MockMvcRequestBuilders.patch("/cart/items/item101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user101\",\"quantity\":20}"))
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Requested quantity 21 exceeds available stock 20 for product meal101"));
    }
}
