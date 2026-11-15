package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.exception.OrderDoesNotExistException;
import com.tw.joi.delivery.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final CartService cartService;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderService(
            CartService cartService,
            ProductService productService,
            OrderRepository orderRepository,
            Clock clock
    ) {
        this.cartService = cartService;
        this.productService = productService;
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    public Order placeOrder(String userId) {
        Cart cart = cartService.getCartForUser(userId);

        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot place an order without a nonempty cart"
            );
        }

        if (cart.getOutlet() == null) {
            throw new IllegalStateException("Cart must have an outlet");
        }

        String outletId = cart.getOutlet().getOutletId();

        List<CartItem> acceptedItems =
                validateAndCopyItems(cart.getCartItems(), outletId);

        Order order = new Order(
                UUID.randomUUID().toString(),
                userId,
                outletId,
                acceptedItems,
                Instant.now(clock)
        );

        orderRepository.save(order);
        cart.getCartItems().clear();

        return order;
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderDoesNotExistException(orderId));
    }

    public Order changeStatus(String orderId, OrderStatus next) {
        Order order = getOrder(orderId);

        order.transitionTo(next);
        orderRepository.save(order);

        return order;
    }

    private List<CartItem> validateAndCopyItems(
            List<CartItem> cartItems,
            String outletId
    ) {
        Map<String, Integer> quantities = new HashMap<>();
        List<CartItem> acceptedItems = new ArrayList<>();

        for (CartItem item : cartItems) {
            GroceryProduct product = productService.getProduct(
                    item.getProductId(), outletId
            );

            int totalQuantity = Math.addExact(
                    quantities.getOrDefault(item.getProductId(), 0),
                    item.getQuantity()
            );

            if (totalQuantity > product.getAvailableStock()) {
                throw new IllegalStateException(
                        "Insufficient stock for product: " + item.getProductId()
                );
            }

            quantities.put(item.getProductId(), totalQuantity);

            BigDecimal currentPrice = product.getUnitPrice();

            if (currentPrice == null || currentPrice.signum() < 0) {
                throw new IllegalStateException(
                        "Invalid catalog price for product: " + item.getProductId()
                );
            }

            if (currentPrice.compareTo(item.getUnitPrice()) != 0) {
                throw new IllegalStateException(
                        "Price changed for product: " + item.getProductId()
                );
            }

            acceptedItems.add(
                    new CartItem(product, item.getQuantity(), currentPrice)
            );
        }

        return acceptedItems;
    }
}