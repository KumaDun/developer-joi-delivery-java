package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Order;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> ordersById = new HashMap<>();

    @Override
    public void save(Order order) {
        ordersById.put(order.getOrderId(), order);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }
}