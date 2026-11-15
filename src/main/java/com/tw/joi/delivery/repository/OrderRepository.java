package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Order;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(String orderId);
}