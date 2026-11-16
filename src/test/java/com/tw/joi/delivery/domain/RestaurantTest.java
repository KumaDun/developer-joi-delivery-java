package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RestaurantTest {

    @Test
    void shouldRegisterMealThroughOutletAndAssociateRestaurant() {
        Restaurant restaurant = restaurant();
        Outlet<FoodProduct> outlet = restaurant;
        FoodProduct meal = meal();

        outlet.addProduct(meal);

        assertSame(meal, restaurant.findProduct("meal101").orElseThrow());
        assertSame(restaurant, meal.getOutlet());
        assertEquals(List.of(meal), restaurant.getMenu());
    }

    @Test
    void shouldRejectDuplicateProductIdWithoutChangingMenu() {
        Restaurant restaurant = restaurant();
        FoodProduct original = meal();
        FoodProduct duplicate = meal();
        restaurant.addProduct(original);

        assertThrows(IllegalArgumentException.class,
                () -> restaurant.addProduct(duplicate));

        assertEquals(List.of(original), restaurant.getMenu());
        assertSame(original, restaurant.findProduct("meal101").orElseThrow());
        assertNull(duplicate.getOutlet());
    }

    @Test
    void shouldRejectMovingMealToAnotherRestaurant() {
        Restaurant first = restaurant();
        Restaurant second = Restaurant.builder().outletId("restaurant102").build();
        FoodProduct meal = meal();
        first.addProduct(meal);

        assertThrows(IllegalArgumentException.class, () -> second.addProduct(meal));

        assertSame(first, meal.getOutlet());
        assertEquals(List.of(meal), first.getMenu());
        assertTrue(second.getMenu().isEmpty());
    }

    @Test
    void shouldAcceptSeparateMealInstancesWithDistinctIdsAtDifferentRestaurants() {
        Restaurant first = restaurant();
        Restaurant second = Restaurant.builder().outletId("restaurant102").build();
        FoodProduct firstMeal = meal();
        FoodProduct secondMeal = meal();
        secondMeal.setProductId("meal102");

        first.addProduct(firstMeal);
        second.addProduct(secondMeal);

        assertSame(firstMeal, first.findProduct("meal101").orElseThrow());
        assertSame(secondMeal, second.findProduct("meal102").orElseThrow());
        assertSame(first, firstMeal.getOutlet());
        assertSame(second, secondMeal.getOutlet());
    }

    @Test
    void shouldRejectMealAssignedToAnotherRestaurantBeforeRegistration() {
        Restaurant owner = restaurant();
        Restaurant other = Restaurant.builder().outletId("restaurant102").build();
        FoodProduct meal = FoodProduct.builder()
                .productId("meal101").restaurant(owner).build();

        assertThrows(IllegalArgumentException.class, () -> other.addProduct(meal));

        assertSame(owner, meal.getOutlet());
        assertTrue(other.getMenu().isEmpty());
    }

    @Test
    void shouldExposeMenuAsReadOnlySnapshot() {
        Restaurant restaurant = restaurant();
        List<FoodProduct> emptyMenu = restaurant.getMenu();
        FoodProduct meal = meal();

        assertThrows(UnsupportedOperationException.class, () -> emptyMenu.add(meal));
        restaurant.addProduct(meal);

        assertTrue(emptyMenu.isEmpty());
        assertEquals(List.of(meal), restaurant.getMenu());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectInvalidProductIdBeforeChangingOwnership(String productId) {
        Restaurant restaurant = restaurant();
        FoodProduct meal = meal();
        meal.setProductId(productId);

        assertThrows(IllegalArgumentException.class, () -> restaurant.addProduct(meal));

        assertTrue(restaurant.getMenu().isEmpty());
        assertNull(meal.getOutlet());
    }

    @Test
    void shouldRejectNullProductAndMissingOutletId() {
        Restaurant restaurant = restaurant();
        assertThrows(IllegalArgumentException.class, () -> restaurant.addProduct(null));

        Restaurant missingId = new Restaurant();
        FoodProduct meal = meal();
        assertThrows(IllegalArgumentException.class, () -> missingId.addProduct(meal));

        assertTrue(missingId.getMenu().isEmpty());
        assertNull(meal.getOutlet());
    }

    private Restaurant restaurant() {
        return Restaurant.builder()
                .outletId("restaurant101")
                .name("Spice Kitchen")
                .build();
    }

    private FoodProduct meal() {
        return FoodProduct.builder()
                .productId("meal101")
                .productName("Chicken Curry")
                .mrp(new BigDecimal("25.00"))
                .unitPrice(new BigDecimal("25.00"))
                .availableStock(20)
                .build();
    }
}
