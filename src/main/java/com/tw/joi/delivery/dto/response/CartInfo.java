package com.tw.joi.delivery.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.User;
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

        public static OutletInfo from(Outlet outlet) {
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

    public record ProductInfo(
            String productId,
            String productName,
            BigDecimal mrp,
            BigDecimal sellingPrice,
            BigDecimal weight,
            int expiryDate,
            int threshold,
            int availableStock,
            BigDecimal discount,
            OutletInfo store
    ) {

        public static ProductInfo from(GroceryProduct product) {
            return new ProductInfo(
                    product.getProductId(),
                    product.getProductName(),
                    product.getMrp(),
                    product.getSellingPrice(),
                    product.getWeight(),
                    product.getExpiryDate(),
                    product.getThreshold(),
                    product.getAvailableStock(),
                    product.getDiscount(),
                    OutletInfo.from(product.getStore())
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
                GroceryProduct product) {
            return new CartItemInfo(
                    ProductInfo.from(product),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice()
            );
        }
    }
}