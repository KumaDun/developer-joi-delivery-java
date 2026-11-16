package com.tw.joi.delivery.seedData;

import com.tw.joi.delivery.domain.*;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SeedData {

    public static GroceryStore store101 = SeedData.createStore("Fresh Picks", "store101");
    public static GroceryStore store102 = SeedData.createStore("Natural Choice", "store102");

    public static Restaurant restaurant101 = Restaurant.builder()
            .name("Spice Kitchen")
            .outletId("restaurant101")
            .build();

    public static User user101= SeedData.createUser("user101", "John", "Doe");
    public static User user102 = SeedData.createUser("user102", "Rachel", "Zane");

    public static List<GroceryProduct> groceryProducts =
        Arrays.asList(createGroceryProduct("Wheat Bread", "product101", store101),
                      createGroceryProduct("Spinach", "product102", store101),
                      createGroceryProduct("Crackers", "product103", store101));

    public static List<FoodProduct> foodProducts = Arrays.asList(
            createFoodProduct("Chicken Curry", "meal101", new BigDecimal("25.00"), restaurant101),
            createFoodProduct("Vegetable Curry", "meal102", new BigDecimal("18.00"), restaurant101));

    public static List<User> users = Arrays.asList(user101, user102);
    public static List<Outlet<?>> outlets = List.of(store101, store102, restaurant101);

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
