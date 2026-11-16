package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.StockStatus;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse.ProductHealth;
import com.tw.joi.delivery.dto.response.InventoryStockResponse;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.service.InventoryHealthService;
import com.tw.joi.delivery.service.InventoryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryHealthService inventoryHealthService;

    @MockitoBean
    private InventoryService inventoryService;

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

    @Test
    void shouldSetStockAndReturnUpdatedSnapshot() throws Exception {
        when(inventoryService.setStock("store101", "product101", 40))
                .thenReturn(new InventoryStockResponse("store101", "product101", 40));

        mockMvc.perform(put("/inventory/store101/products/product101/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableStock\":40}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"outletId":"store101","productId":"product101","availableStock":40}
                        """, JsonCompareMode.STRICT));

        verify(inventoryService).setStock("store101", "product101", 40);
    }

    @Test
    void shouldAdjustRestaurantStockAndReturnUpdatedSnapshot() throws Exception {
        when(inventoryService.adjustStock("restaurant101", "meal101", -3))
                .thenReturn(new InventoryStockResponse("restaurant101", "meal101", 17));

        mockMvc.perform(post("/inventory/restaurant101/products/meal101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-3}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"outletId":"restaurant101","productId":"meal101","availableStock":17}
                        """, JsonCompareMode.STRICT));

        verify(inventoryService).adjustStock("restaurant101", "meal101", -3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"availableStock\":null}", "{\"availableStock\":-1}",
            "{\"availableStock\":2147483648}"})
    void shouldRejectInvalidSetStockBodyBeforeCallingService(String body) throws Exception {
        mockMvc.perform(put("/inventory/store101/products/product101/stock")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"delta\":null}", "{\"delta\":2147483648}"})
    void shouldRejectInvalidAdjustmentBodyBeforeCallingService(String body) throws Exception {
        mockMvc.perform(post("/inventory/store101/products/product101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldReturnConflictWhenAdjustmentWouldMakeStockNegative() throws Exception {
        when(inventoryService.adjustStock("store101", "product101", -3))
                .thenThrow(new InsufficientStockException("product101", 3, 2));

        mockMvc.perform(post("/inventory/store101/products/product101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Requested quantity 3 exceeds available stock 2 for product product101"));
    }
}
