package com.tw.joi.delivery.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.json.JsonCompareMode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryHealthResponseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReportSeededGroceryInventoryInProductIdOrder() throws Exception {
        mockMvc.perform(get("/inventory/health").param("storeId", "store101"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "storeId": "store101",
                          "storeName": "Fresh Picks",
                          "products": [
                            {"productId":"product101","productName":"Wheat Bread",
                             "availableStock":30,"threshold":10,"status":"HEALTHY"},
                            {"productId":"product102","productName":"Spinach",
                             "availableStock":30,"threshold":10,"status":"HEALTHY"},
                            {"productId":"product103","productName":"Crackers",
                             "availableStock":30,"threshold":10,"status":"HEALTHY"}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void shouldReportEmptySeededStoreWithoutProducts() throws Exception {
        mockMvc.perform(get("/inventory/health").param("storeId", "store102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value("store102"))
                .andExpect(jsonPath("$.storeName").value("Natural Choice"))
                .andExpect(jsonPath("$.products").isEmpty());
    }

    @Test
    void shouldReturnNotFoundForUnknownStore() throws Exception {
        mockMvc.perform(get("/inventory/health").param("storeId", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store: missing does not exist"));
    }

    @Test
    void shouldRejectSeededRestaurant() throws Exception {
        mockMvc.perform(get("/inventory/health").param("storeId", "restaurant101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Inventory health is only available for grocery stores"));
    }
}
