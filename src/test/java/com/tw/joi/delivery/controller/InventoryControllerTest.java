package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.StockStatus;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse.ProductHealth;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.service.InventoryHealthService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryHealthService inventoryHealthService;

    @Test
    void shouldReturnTheHealthOfTheRequestedStore() throws Exception {
        when(inventoryHealthService.getHealth("store101")).thenReturn(new InventoryHealthResponse(
                "store101",
                "Fresh Picks",
                List.of(
                        new ProductHealth("product101", "Wheat Bread", 0, 10, StockStatus.OUT_OF_STOCK),
                        new ProductHealth("product102", "Spinach", 5, 10, StockStatus.LOW_STOCK),
                        new ProductHealth("product103", "Crackers", 30, 10, StockStatus.HEALTHY))));

        String getUrl = "/inventory/health?storeId={storeId}";
        mockMvc.perform(get(getUrl, "store101").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "storeId": "store101",
                          "storeName": "Fresh Picks",
                          "products": [
                            {"productId":"product101","productName":"Wheat Bread",
                             "availableStock":0,"threshold":10,"status":"OUT_OF_STOCK"},
                            {"productId":"product102","productName":"Spinach",
                             "availableStock":5,"threshold":10,"status":"LOW_STOCK"},
                            {"productId":"product103","productName":"Crackers",
                             "availableStock":30,"threshold":10,"status":"HEALTHY"}
                          ]
                        }
                        """, JsonCompareMode.STRICT));

        verify(inventoryHealthService).getHealth("store101");
    }

    @Test
    void shouldReturnEmptyProductListForEmptyStore() throws Exception {
        when(inventoryHealthService.getHealth("store102"))
                .thenReturn(new InventoryHealthResponse("store102", "Natural Choice", List.of()));

        mockMvc.perform(get("/inventory/health").param("storeId", "store102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value("store102"))
                .andExpect(jsonPath("$.storeName").value("Natural Choice"))
                .andExpect(jsonPath("$.products").isEmpty());

        verify(inventoryHealthService).getHealth("store102");
    }

    @Test
    void shouldRejectMissingStoreIdWithoutCallingService() throws Exception {
        mockMvc.perform(get("/inventory/health"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryHealthService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void shouldRejectBlankStoreIdWithoutCallingService(String storeId) throws Exception {
        mockMvc.perform(get("/inventory/health").param("storeId", storeId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryHealthService);
    }

    @Test
    void shouldReturnNotFoundForUnknownStore() throws Exception {
        when(inventoryHealthService.getHealth("missing"))
                .thenThrow(new StoreIdDoesNotExistException("missing"));

        mockMvc.perform(get("/inventory/health").param("storeId", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store: missing does not exist"));

        verify(inventoryHealthService).getHealth("missing");
    }

    @Test
    void shouldRejectRestaurantInventoryHealth() throws Exception {
        when(inventoryHealthService.getHealth("restaurant101"))
                .thenThrow(new IllegalArgumentException(
                        "Inventory health is only available for grocery stores"));

        mockMvc.perform(get("/inventory/health").param("storeId", "restaurant101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Inventory health is only available for grocery stores"));

        verify(inventoryHealthService).getHealth("restaurant101");
    }
}
