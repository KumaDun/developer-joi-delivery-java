package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.User;

public interface CartRepository {

    Cart getOrCreateForUser(User user);
}
