package com.tw.joi.delivery.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.service.CartService;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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

        when(cartService.getCartForUser(userId))
                .thenThrow(new UserDoesNotExistException(userId));

        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                        .param("userId", userId))
                .andExpect(status().isNotFound());

        verify(cartService).getCartForUser(userId);
    }

    @Test
    void shouldAddTheRequestedProductToTheCart() throws Exception {

        String url = "/cart/product";
        AddProductRequest addProductRequest = new AddProductRequest();
        addProductRequest.setQuantity(1);
        addProductRequest.setProductId("product101");
        addProductRequest.setUserId("user101");
        addProductRequest.setOutletId("store101");

        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(addProductRequest );

        mockMvc.perform(MockMvcRequestBuilders.post(url)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnTheCart() throws Exception {
        String url = "/cart/view?userId={userId}";
        String userId="user101";
        Cart cart= Cart.builder()
            .cartId("cart101")
            .build();
        when(cartService.getCartForUser(userId)).thenReturn(cart);

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
}