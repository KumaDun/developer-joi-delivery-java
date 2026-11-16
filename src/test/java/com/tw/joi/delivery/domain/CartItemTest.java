package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    @ParameterizedTest
    @EnumSource(SpiceLevel.class)
    void shouldKeepMealSelectionsWithoutChangingPriceOrOtherItems(SpiceLevel spiceLevel) {
        FoodProduct meal = meal();

        CartItem customized = new CartItem(meal, 2, meal.getUnitPrice(), spiceLevel, true);
        CartItem standard = new CartItem(meal, 1);

        assertEquals(spiceLevel, customized.getSpiceLevel());
        assertEquals(Boolean.TRUE, customized.getUtensilsNeeded());
        assertEquals(new BigDecimal("50.00"), customized.getTotalPrice());
        assertEquals(SpiceLevel.NONE, standard.getSpiceLevel());
        assertEquals(Boolean.FALSE, standard.getUtensilsNeeded());
        assertEquals(new BigDecimal("25.00"), meal.getUnitPrice());
    }

    @Test
    void shouldDefaultMealOptionsForBothExistingConstructors() {
        FoodProduct meal = meal();

        CartItem implicitPrice = new CartItem(meal, 1);
        CartItem explicitPrice = new CartItem(meal, 1, new BigDecimal("25.00"));

        assertEquals(SpiceLevel.NONE, implicitPrice.getSpiceLevel());
        assertEquals(Boolean.FALSE, implicitPrice.getUtensilsNeeded());
        assertEquals(SpiceLevel.NONE, explicitPrice.getSpiceLevel());
        assertEquals(Boolean.FALSE, explicitPrice.getUtensilsNeeded());
    }

    @Test
    void shouldRejectRestaurantOptionsForGroceriesIncludingExplicitFalse() {
        GroceryProduct grocery = product();

        assertThrows(IllegalArgumentException.class,
                () -> new CartItem(grocery, 1, BigDecimal.ONE, SpiceLevel.NONE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CartItem(grocery, 1, BigDecimal.ONE, null, true));
        assertThrows(IllegalArgumentException.class,
                () -> new CartItem(grocery, 1, BigDecimal.ONE, null, false));
    }

    @Test
    void shouldLeaveCustomizationAbsentForGroceries() {
        CartItem item = new CartItem(product(), 1, BigDecimal.ONE);

        assertNull(item.getSpiceLevel());
        assertNull(item.getUtensilsNeeded());
    }

    private FoodProduct meal() {
        return FoodProduct.builder()
                .productId("meal101")
                .productName("Chicken Curry")
                .unitPrice(new BigDecimal("25.00"))
                .build();
    }

    private GroceryProduct product() {
        return GroceryProduct.builder()
                .productId("product101")
                .productName("Wheat Bread")
                .build();
    }
}
