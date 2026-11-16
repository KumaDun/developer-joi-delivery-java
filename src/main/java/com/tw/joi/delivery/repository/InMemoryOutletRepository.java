package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Outlet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOutletRepository implements StoreRepository {

    private final Map<String, Outlet<?>> storesById = new ConcurrentHashMap<>();

    public InMemoryOutletRepository(Collection<? extends Outlet<?>> stores) {
        for (Outlet<?> store : stores) {
            save(store);
        }
    }

    @Override
    public Optional<Outlet<?>> findById(String storeId) {
        return Optional.ofNullable(storesById.get(storeId));
    }

    @Override
    public Optional<Outlet<?>> findByName(String storeName) {
        return storesById.values().stream()
                .filter(store -> Objects.equals(store.getName(), storeName))
                .findFirst();
    }

    @Override
    public void save(Outlet<?> store) {
        storesById.put(store.getOutletId(), store);
    }


    @Override
    public Collection<Outlet<?>> findAll() {
        return List.copyOf(storesById.values());
    }
}
