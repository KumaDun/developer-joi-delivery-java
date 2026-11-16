package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Outlet;

import java.util.Collection;
import java.util.Optional;

public interface StoreRepository {
    Optional<Outlet<?>> findById(String storeId);

    Optional<Outlet<?>> findByName(String storeName);

    void save(Outlet<?> store);

    Collection<Outlet<?>> findAll();
}
