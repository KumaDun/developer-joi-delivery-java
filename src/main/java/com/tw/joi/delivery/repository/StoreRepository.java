package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Outlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface StoreRepository {
    Optional<Outlet<?>> findById(String storeId);

    Optional<Outlet<?>> findByName(String storeName);

    void save(Outlet<?> store);

    final Map<String, Outlet<?>> storesById = new HashMap<>();

    Collection<Outlet<?>> findAll();
}