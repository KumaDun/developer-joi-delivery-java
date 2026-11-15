package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.response.CartProductInfo;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {

    private final Map<String,Cart> userCarts= SeedData.cartForUsers;
    private final UserService userService;
    private final ProductService productService;

    public CartProductInfo addProductToCartForUser(AddProductRequest addProductRequest) {
        User user=userService.fetchUserById(addProductRequest.getUserId());
        int quantity = addProductRequest.getQuantity();
        Cart cart = fetchCartForUser(user);
        GroceryProduct product = productService.getProduct(addProductRequest.getProductId(),
                                                           addProductRequest.getOutletId());
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
        CartItem cartItem = new CartItem(product,quantity);
        cart.getCartItems().add(cartItem);
        return new CartProductInfo(cart, product, product.getSellingPrice());
    }

    public Cart getCartForUser(String userId) {
        User user=userService.fetchUserById(userId);
        return fetchCartForUser(user);
    }

    private Cart fetchCartForUser(User user) {
        return userCarts.get(user.getUserId());
    }

}
