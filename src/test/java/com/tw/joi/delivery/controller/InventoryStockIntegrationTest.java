package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.seedData.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryStockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Product grocery;
    private Product meal;
    private int groceryStock;
    private int mealStock;

    @BeforeEach
    void saveStock() {
        grocery = SeedData.store101.findProduct("product101").orElseThrow();
        meal = SeedData.restaurant101.findProduct("meal101").orElseThrow();
        synchronized (grocery) {
            groceryStock = grocery.getAvailableStock();
        }
        synchronized (meal) {
            mealStock = meal.getAvailableStock();
        }
    }

    @AfterEach
    void restoreStock() {
        synchronized (grocery) {
            grocery.setAvailableStock(groceryStock);
        }
        synchronized (meal) {
            meal.setAvailableStock(mealStock);
        }
    }

    @Test
    void shouldUpdateGroceryStockAndReflectItInHealth() throws Exception {
        mockMvc.perform(put("/inventory/store101/products/product101/stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outletId").value("store101"))
                .andExpect(jsonPath("$.productId").value("product101"))
                .andExpect(jsonPath("$.availableStock").value(9));
        mockMvc.perform(get("/inventory/health").param("storeId", "store101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].availableStock").value(9))
                .andExpect(jsonPath("$.products[0].status").value("LOW_STOCK"));

        mockMvc.perform(post("/inventory/store101/products/product101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(0));
        mockMvc.perform(get("/inventory/health").param("storeId", "store101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].availableStock").value(0))
                .andExpect(jsonPath("$.products[0].status").value("OUT_OF_STOCK"));
        assertEquals(mealStock, meal.getAvailableStock());
    }

    @Test
    void shouldSetAndAdjustMealStockIndependentlyOfGroceries() throws Exception {
        mockMvc.perform(put("/inventory/restaurant101/products/meal101/stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(7));
        mockMvc.perform(post("/inventory/restaurant101/products/meal101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outletId").value("restaurant101"))
                .andExpect(jsonPath("$.productId").value("meal101"))
                .andExpect(jsonPath("$.availableStock").value(5));

        assertEquals(5, meal.getAvailableStock());
        assertEquals(groceryStock, grocery.getAvailableStock());
    }

    @Test
    void shouldRejectUnderflowWithoutChangingStock() throws Exception {
        mockMvc.perform(put("/inventory/store101/products/product101/stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/inventory/store101/products/product101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Requested quantity 3 exceeds available stock 2 for product product101"));

        assertEquals(2, grocery.getAvailableStock());
    }

    @Test
    void shouldRejectZeroAdjustmentAndNegativeTargetWithoutChangingStock() throws Exception {
        mockMvc.perform(post("/inventory/store101/products/product101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/inventory/store101/products/product101/stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":-1}"))
                .andExpect(status().isBadRequest());

        assertEquals(groceryStock, grocery.getAvailableStock());
    }

    @Test
    void shouldRejectOverflowWithoutChangingMaximumStock() throws Exception {
        mockMvc.perform(put("/inventory/store101/products/product101/stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":2147483647}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/inventory/store101/products/product101/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":1}"))
                .andExpect(status().isBadRequest());

        assertEquals(Integer.MAX_VALUE, grocery.getAvailableStock());
    }

    @ParameterizedTest
    @CsvSource({"missing, product101", "store101, missing-product", "store102, product101"})
    void shouldRejectUnknownOrWrongOutletProducts(String outletId, String productId) throws Exception {
        mockMvc.perform(put("/inventory/{outletId}/products/{productId}/stock", outletId, productId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"availableStock\":5}"))
                .andExpect(status().isNotFound());

        assertEquals(groceryStock, grocery.getAvailableStock());
        assertEquals(mealStock, meal.getAvailableStock());
    }
}
