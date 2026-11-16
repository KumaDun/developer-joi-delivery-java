package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.Assert;

public class InMemoryUserRepository implements UserRepository{
    private final Map<String, User> usersById = new ConcurrentHashMap<>();

    public InMemoryUserRepository(List<User> users) {
        for (User user : users) {
            save(user);
        }
    }

    @Override
    public Optional<User> findById(String userId) {
        Assert.hasText(userId, "User ID must not be blank");
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public void save(User user) {
        Assert.notNull(user, "User must not be null");
        Assert.hasText(user.getUserId(), "User ID must not be blank");
        usersById.put(user.getUserId(), user);
    }

    @Override
    public Collection<User> findAll() {
        return List.copyOf(usersById.values());
    }
}
