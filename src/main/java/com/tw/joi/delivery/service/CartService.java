package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.*;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.response.CartInfo;
import com.tw.joi.delivery.dto.response.CartInfo.OutletInfo;
import com.tw.joi.delivery.dto.response.CartInfo.ProductInfo;
import com.tw.joi.delivery.dto.response.CartInfo.UserInfo;
import com.tw.joi.delivery.dto.response.CartProductInfo;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CartService {

    // TODO do not user SeedData in service
    private final Map<String,Cart> userCarts= SeedData.cartForUsers;
    private final UserService userService;
    private final ProductService productService;

    public CartProductInfo addProductToCartForUser(AddProductRequest addProductRequest) {
        User user=userService.fetchUserById(addProductRequest.getUserId());
        Cart cart = fetchCartForUser(user);
        Product product = productService.getProduct(addProductRequest.getProductId(),
                                                           addProductRequest.getOutletId());

        int quantity = addProductRequest.getQuantity();

        long existingQuantity = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(product.getProductId()))
                .mapToLong(CartItem::getQuantity)
                .sum();

        long totalQuantity = existingQuantity + quantity;

        if (totalQuantity > product.getAvailableStock()) {
            throw new IllegalStateException(
                    "Insufficient stock for product: " + product.getProductId()
            );
        }
        CartItem newItem = new CartItem(product,quantity);

        // Build the response before changing the cart so a failed
        // catalog lookup does not leave a partially completed addition.
        List<CartInfo.CartItemInfo> products = new ArrayList<>();

        for (CartItem existingItem : cart.getCartItems()) {
            Product existingProduct;

            if (existingItem.getProductId().equals(product.getProductId())) {
                existingProduct = product;
            } else {
                existingProduct = productService.getProduct(
                        existingItem.getProductId(),
                        cart.getOutlet().getOutletId()
                );
            }

            products.add(
                    CartInfo.CartItemInfo.from(existingItem, existingProduct)
            );
        }

        products.add(CartInfo.CartItemInfo.from(newItem, product));

        CartInfo cartInfo = new CartInfo(
                cart.getCartId(),
                OutletInfo.from(cart.getOutlet()),
                List.copyOf(products),
                UserInfo.from(cart.getUser())
        );

        CartProductInfo response = new CartProductInfo(
                cartInfo,
                ProductInfo.from(product),
                product.getUnitPrice()
        );

        cart.getCartItems().add(newItem);

        return response;
    }

    public CartInfo getCartInfoForUser(String userId) {
        Cart cart = getCartForUser(userId);

        List<CartInfo.CartItemInfo> products = cart.getCartItems().stream()
                .map(item -> {
                    Product product = productService.getProduct(
                            item.getProductId(),
                            cart.getOutlet().getOutletId()
                    );

                    return CartInfo.CartItemInfo.from(item, product);
                })
                .toList();

        return new CartInfo(
                cart.getCartId(),
                OutletInfo.from(cart.getOutlet()),
                products,
                UserInfo.from(cart.getUser())
        );
    }

    public Cart getCartForUser(String userId) {
        User user=userService.fetchUserById(userId);
        return fetchCartForUser(user);
    }

    private Cart fetchCartForUser(User user) {
        return userCarts.get(user.getUserId());
    }

}
