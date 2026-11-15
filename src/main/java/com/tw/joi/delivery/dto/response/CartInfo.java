package com.tw.joi.delivery.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tw.joi.delivery.domain.*;

import java.math.BigDecimal;
import java.util.List;

public record CartInfo(
        String cartId,
        OutletInfo outlet,
        List<CartItemInfo> products,
        UserInfo user
) {

    public record OutletInfo(
            String name,
            String description,
            String outletId,
            List<ProductInfo> inventory
    ) {

        public static OutletInfo from(Outlet<?> outlet) {
            if (outlet == null) {
                return null;
            }

            return new OutletInfo(
                    outlet.getName(),
                    outlet.getDescription(),
                    outlet.getOutletId(),
                    List.of()
            );
        }
    }

    public record UserInfo(
            String userId,
            String username,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            CartInfo cart
    ) {

        public static UserInfo from(User user) {
            if (user == null) {
                return null;
            }

            return new UserInfo(
                    user.getUserId(),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    null
            );
        }
    }

    public enum ProductType {
        GROCERY,
        MEAL
    }

    public record GroceryDetails(
            BigDecimal weight,
            int expiryDate,
            int threshold,
            BigDecimal discount,
            OutletInfo store
    ) {

        public static GroceryDetails from(GroceryProduct product) {
            return new GroceryDetails(
                    product.getWeight(),
                    product.getExpiryDate(),
                    product.getThreshold(),
                    product.getDiscount(),
                    OutletInfo.from(product.getStore())
            );
        }
    }

    public record ProductInfo(
            String productId,
            String productName,
            BigDecimal mrp,
            BigDecimal catalogUnitPrice,
            int availableStock,
            ProductType productType,
            GroceryDetails groceryDetails
    ) {

        public static ProductInfo from(Product product) {
            if (product instanceof GroceryProduct grocery) {
                return new ProductInfo(
                        grocery.getProductId(),
                        grocery.getProductName(),
                        grocery.getMrp(),
                        grocery.getUnitPrice(),
                        grocery.getAvailableStock(),
                        ProductType.GROCERY,
                        GroceryDetails.from(grocery)
                );
            }

            if (product instanceof FoodProduct meal) {
                return new ProductInfo(
                        meal.getProductId(),
                        meal.getProductName(),
                        meal.getMrp(),
                        meal.getUnitPrice(),
                        meal.getAvailableStock(),
                        ProductType.MEAL,
                        null
                );
            }

            throw new IllegalArgumentException(
                    "Unsupported product type"
            );
        }
    }

    public record CartItemInfo(
            @JsonUnwrapped ProductInfo product,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {

        public static CartItemInfo from(
                CartItem item,
                Product product) {
            return new CartItemInfo(
                    ProductInfo.from(product),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice()
            );
        }
    }
}