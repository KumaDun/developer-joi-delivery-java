package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.repository.CartRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CartService {

    private final UserService userService;
    private final ProductService productService;
    private final CartRepository cartRepository;

    public CartItem addProductToCartForUser(AddProductRequest addProductRequest) {
        Cart cart = getCartForUser(addProductRequest.getUserId());
        synchronized (cart) {
            return addProductToCart(cart, addProductRequest);
        }
    }

    private CartItem addProductToCart(Cart cart, AddProductRequest addProductRequest) {
        Product product = productService.getProduct(addProductRequest.getProductId(),
                                                           addProductRequest.getOutletId());

        Outlet<?> outlet = product.getOutlet();
        if (outlet == null || !addProductRequest.getOutletId().equals(outlet.getOutletId())) {
            throw new IllegalArgumentException("Product does not belong to the requested outlet");
        }

        if (!cart.getCartItems().isEmpty()
                && (cart.getOutlet() == null
                    || !outlet.getOutletId().equals(cart.getOutlet().getOutletId()))) {
            throw new IllegalArgumentException("Cart can only contain products from one outlet");
        }

        int quantity = addProductRequest.getQuantity();

        long existingQuantity = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(product.getProductId()))
                .mapToLong(CartItem::getQuantity)
                .sum();

        long totalQuantity = existingQuantity + quantity;

        if (totalQuantity > product.getAvailableStock()) {
            throw new InsufficientStockException(
                    product.getProductId(), totalQuantity, product.getAvailableStock()
            );
        }
        CartItem newItem = new CartItem(product, quantity, product.getUnitPrice(),
                addProductRequest.getSpiceLevel(), addProductRequest.getUtensilsNeeded());
        cart.getCartItems().add(newItem);
        cart.setOutlet(outlet);

        return newItem;
    }

    /** Returns the canonical cart; internal callers must hold its monitor when accessing its state. */
    public Cart getCartForUser(String userId) {
        User user=userService.fetchUserById(userId);
        return cartRepository.getOrCreateForUser(user);
    }

    /** Captures a detached item-list snapshot for serialization outside the cart lock. */
    public Cart getCartSnapshotForUser(String userId) {
        Cart cart = getCartForUser(userId);
        synchronized (cart) {
            return Cart.builder()
                    .cartId(cart.getCartId())
                    .outlet(cart.getOutlet())
                    .cartItems(List.copyOf(cart.getCartItems()))
                    .user(cart.getUser())
                    .build();
        }
    }

}
