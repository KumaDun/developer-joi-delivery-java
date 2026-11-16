package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.User;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String userId);

    void save(User user);

    Collection<User> findAll();
}
