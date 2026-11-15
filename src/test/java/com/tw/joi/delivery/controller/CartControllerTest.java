package com.tw.joi.delivery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.joi.delivery.domain.*;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.response.CartInfo;
import com.tw.joi.delivery.dto.response.CartProductInfo;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.service.CartService;
import com.tw.joi.delivery.service.ProductService;
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
    private ProductService productService;

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

        when(cartService.getCartInfoForUser(userId))
                .thenThrow(new UserDoesNotExistException(userId));

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                        .param("userId", userId))
                .andExpect(status().isNotFound());

        verify(cartService).getCartInfoForUser(userId);
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

        CartInfo cartInfo = new CartInfo(
                "cart101",
                null,
                List.of(CartInfo.CartItemInfo.from(item, product)),
                null
        );

        CartProductInfo response = new CartProductInfo(
                cartInfo,
                CartInfo.ProductInfo.from(product),
                new BigDecimal("10.50")
        );

        when(cartService.addProductToCartForUser(any(AddProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cart.cartId")
                        .value("cart101"))
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.cart.products[0].productId").value("product101"))
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.cart.products[0].quantity").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.cart.products[0].unitPrice").value(10.5))
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.cart.products[0].totalPrice").value(21.0))
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.cart.products[0].product").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$.cart.cartItems").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.product.productId")
                        .value("product101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sellingPrice")
                        .value(10.5));

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
    void shouldReturnTheCart() throws Exception {
        String url = "/cart/view?userId={userId}";
        String userId="user101";
        CartInfo cartInfo = new CartInfo(
                "cart101", null, List.of(), null
        );

        when(cartService.getCartInfoForUser(userId))
                .thenReturn(cartInfo);

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
    void shouldReturnCartProductsWithQuantityAndPrices() throws Exception {
        GroceryProduct product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .sellingPrice(new BigDecimal("10.50"))
                .build();

        CartItem item = new CartItem(product, 2);

        CartInfo cartInfo = new CartInfo(
                "cart101",
                null,
                List.of(CartInfo.CartItemInfo.from(item, product)),
                null
        );

        when(cartService.getCartInfoForUser("user101"))
                .thenReturn(cartInfo);

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                        .param("userId", "user101"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartId")
                        .value("cart101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].productId")
                        .value("product101"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].quantity")
                        .value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].unitPrice")
                        .value(10.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].totalPrice")
                        .value(21.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cartItems")
                        .doesNotExist());

        verify(cartService).getCartInfoForUser("user101");
    }
}