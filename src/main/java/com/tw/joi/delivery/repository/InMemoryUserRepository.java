package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.User;

import java.util.*;

public class InMemoryUserRepository implements UserRepository{
    private final Map<String, User> usersById = new HashMap<>();

    public InMemoryUserRepository(List<User> users) {
        for (User user : users) {
            usersById.put(user.getUserId(), user);
        }
    }

    @Override
    public Optional<User> findById(String userId) {
        return usersById.values().stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public Collection<User> findAll() {
        return Collections.unmodifiableCollection(usersById.values());
    }
}
