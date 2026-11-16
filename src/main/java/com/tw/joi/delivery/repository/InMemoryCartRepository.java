package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.Assert;

public class InMemoryCartRepository implements CartRepository {

    private final Map<String, Cart> cartsByUserId = new ConcurrentHashMap<>();

    @Override
    public Cart getOrCreateForUser(User user) {
        Assert.notNull(user, "User must not be null");
        Assert.hasText(user.getUserId(), "User ID must not be blank");

        return cartsByUserId.computeIfAbsent(user.getUserId(), userId ->
                Cart.builder()
                        .cartId(UUID.randomUUID().toString())
                        .user(user)
                        .build());
    }
}
