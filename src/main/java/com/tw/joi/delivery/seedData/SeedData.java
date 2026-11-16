package com.tw.joi.delivery.seedData;

import com.tw.joi.delivery.domain.*;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import lombok.Getter;

@Getter
public final class SeedData {

    private final List<User> users;
    private final List<Outlet<?>> outlets;

    private SeedData(List<User> users, List<Outlet<?>> outlets) {
        this.users = users;
        this.outlets = outlets;
    }

    public static SeedData create() {
        GroceryStore store101 = createStore("Fresh Picks", "store101");
        GroceryStore store102 = createStore("Natural Choice", "store102");
        Restaurant restaurant101 = Restaurant.builder()
                .name("Spice Kitchen")
                .outletId("restaurant101")
                .build();

        createGroceryProduct("Wheat Bread", "product101", store101);
        createGroceryProduct("Spinach", "product102", store101);
        createGroceryProduct("Crackers", "product103", store101);

        createFoodProduct("Chicken Curry", "meal101", new BigDecimal("25.00"), restaurant101);
        createFoodProduct("Vegetable Curry", "meal102", new BigDecimal("18.00"), restaurant101);

        return new SeedData(
                List.of(createUser("user101", "John", "Doe"), createUser("user102", "Rachel", "Zane")),
                List.of(store101, store102, restaurant101)
        );
    }

    public static GroceryStore createStore(String outletName, String storeId) {
        return GroceryStore.builder()
            .name(outletName)
            .outletId(storeId)
            .build();
    }

    public static User createUser(String userId, String firstName, String lastName) {
        return User.builder()
            .userId(userId)
            .firstName(firstName)
            .lastName(lastName)
            .email(firstName + "." + lastName + "@gmail.com")
            .phoneNumber(String.valueOf(SeedData.getRandomNumberUsingNextInt(100000000, 900000000)))
            .build();
    }

    public static int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    private static GroceryProduct createGroceryProduct(String productName,
                                                       String productId, @Nonnull GroceryStore store) {
        GroceryProduct product = GroceryProduct.builder()
                .productName(productName)
                .productId(productId)
                .mrp(BigDecimal.valueOf(10.5))
                .sellingPrice(new BigDecimal("10.50"))
                .weight(BigDecimal.valueOf(500.00))
                .threshold(10)
                .availableStock(30)
                .build();

        store.addProduct(product);
        return product;
    }

    private static FoodProduct createFoodProduct(String productName, String productId,
                                                 BigDecimal unitPrice, Restaurant restaurant) {
        FoodProduct product = FoodProduct.builder()
                .productId(productId)
                .productName(productName)
                .mrp(unitPrice)
                .unitPrice(unitPrice)
                .availableStock(20)
                .build();

        restaurant.addProduct(product);
        return product;
    }

}
