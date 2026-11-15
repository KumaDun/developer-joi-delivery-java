package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    @Test
    void shouldPreserveProductDetailsAndCalculateTotal() {
        GroceryProduct product = GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .build();

        CartItem item = new CartItem(
                product, 3, new BigDecimal("10.50")
        );

        product.setProductId("changed-id");
        product.setProductName("Changed name");

        assertEquals("product101", item.getProductId());
        assertEquals("Wheat Bread", item.getProductName());
        assertEquals(3, item.getQuantity());
        assertEquals(new BigDecimal("31.50"), item.getTotalPrice());
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        GroceryProduct product = product();

        assertThrows(IllegalArgumentException.class,
                () -> new CartItem(product, 0, BigDecimal.ONE));

        assertThrows(IllegalArgumentException.class,
                () -> new CartItem(product, -1, BigDecimal.ONE));
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new CartItem(
                        product(), 1, new BigDecimal("-0.01")
                ));
    }

    @Test
    void shouldAllowZeroPrice() {
        CartItem item = new CartItem(product(), 2, BigDecimal.ZERO);

        assertEquals(0, item.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    private GroceryProduct product() {
        return GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .build();
    }
}