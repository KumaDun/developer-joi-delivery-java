package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.exception.InsufficientStockException;
import com.tw.joi.delivery.exception.CartItemDoesNotExistException;
import com.tw.joi.delivery.repository.CartRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


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

        synchronized (product) {
            return addProductToCart(cart, addProductRequest, product);
        }
    }

    private CartItem addProductToCart(Cart cart, AddProductRequest addProductRequest, Product product) {
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

        int availableStock = product.getAvailableStock();
        if (totalQuantity > availableStock) {
            throw new InsufficientStockException(
                    product.getProductId(), totalQuantity, availableStock
            );
        }
        CartItem newItem = new CartItem(product, quantity, product.getUnitPrice(),
                addProductRequest.getSpiceLevel(), addProductRequest.getUtensilsNeeded());
        cart.getCartItems().add(newItem);
        cart.setOutlet(outlet);

        return newItem;
    }

    public Cart updateCartItemQuantity(String userId, String cartItemId, int quantity) {
        Assert.hasText(cartItemId, "Cart item ID must not be blank");
        Assert.isTrue(quantity >= 0, "Quantity must not be negative");
        Cart cart = getCartForUser(userId);
        synchronized (cart) {
            for (int index = 0; index < cart.getCartItems().size(); index++) {
                CartItem item = cart.getCartItems().get(index);
                if (!item.getCartItemId().equals(cartItemId)) {
                    continue;
                }
                if (quantity == 0) {
                    cart.getCartItems().remove(index);
                    if (cart.getCartItems().isEmpty()) {
                        cart.setOutlet(null);
                    }
                } else if (quantity > item.getQuantity()) {
                    increaseQuantity(cart, index, item, quantity);
                } else if (quantity < item.getQuantity()) {
                    cart.getCartItems().set(index, item.withQuantity(quantity));
                }
                return snapshot(cart);
            }
            throw new CartItemDoesNotExistException(cartItemId);
        }
    }

    private void increaseQuantity(Cart cart, int index, CartItem item, int quantity) {
        if (cart.getOutlet() == null) {
            throw new IllegalStateException("Cart must have an outlet");
        }
        Product product = productService.getProduct(item.getProductId(), cart.getOutlet().getOutletId());
        synchronized (product) {
            if (product.getOutlet() == null
                    || !cart.getOutlet().getOutletId().equals(product.getOutlet().getOutletId())) {
                throw new IllegalArgumentException("Product does not belong to the cart's outlet");
            }
            long existingQuantity = cart.getCartItems().stream()
                    .filter(row -> row.getProductId().equals(item.getProductId()))
                    .mapToLong(CartItem::getQuantity)
                    .sum();
            long requestedQuantity = existingQuantity - item.getQuantity() + quantity;
            int availableStock = product.getAvailableStock();
            if (requestedQuantity > availableStock) {
                throw new InsufficientStockException(item.getProductId(), requestedQuantity, availableStock);
            }
            cart.getCartItems().set(index, item.withQuantity(quantity));
        }
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
            return snapshot(cart);
        }
    }

    private Cart snapshot(Cart cart) {
        return Cart.builder()
                .cartId(cart.getCartId())
                .outlet(cart.getOutlet())
                .cartItems(List.copyOf(cart.getCartItems()))
                .user(cart.getUser())
                .build();
    }

}
