package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.dto.response.LastPurchaseResponse;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.service.LastPurchaseService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LastPurchaseService lastPurchaseService;

    @Test
    void shouldReturnLastPurchaseMetadata() throws Exception {
        when(lastPurchaseService.getLastPurchase("user101")).thenReturn(Optional.of(
                new LastPurchaseResponse("order101", "store101", "Fresh Picks",
                        Instant.parse("2026-11-16T10:00:00Z"))));

        mockMvc.perform(get("/users/{userId}/last-purchase", "user101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order101"))
                .andExpect(jsonPath("$.outletId").value("store101"))
                .andExpect(jsonPath("$.outletName").value("Fresh Picks"))
                .andExpect(jsonPath("$.orderedAt").value("2026-11-16T10:00:00Z"));

        verify(lastPurchaseService).getLastPurchase("user101");
    }

    @Test
    void shouldReturnNoContentWithoutPreviousPurchase() throws Exception {
        when(lastPurchaseService.getLastPurchase("user101")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/{userId}/last-purchase", "user101"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {
        when(lastPurchaseService.getLastPurchase("missing"))
                .thenThrow(new UserDoesNotExistException("missing"));

        mockMvc.perform(get("/users/{userId}/last-purchase", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User: missing does not exist"));
    }

    @Test
    void shouldRejectBlankUserIdBeforeCallingService() throws Exception {
        mockMvc.perform(get("/users/{userId}/last-purchase", " "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lastPurchaseService);
    }
}
