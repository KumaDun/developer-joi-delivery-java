package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.seedData.SeedData;
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

    public CartItem addProductToCartForUser(AddProductRequest addProductRequest) {
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
        CartItem newItem = new CartItem(product, quantity);
        cart.getCartItems().add(newItem);

        return newItem;
    }

    public Cart getCartForUser(String userId) {
        User user=userService.fetchUserById(userId);
        return fetchCartForUser(user);
    }

    private Cart fetchCartForUser(User user) {
        return userCarts.get(user.getUserId());
    }

}
