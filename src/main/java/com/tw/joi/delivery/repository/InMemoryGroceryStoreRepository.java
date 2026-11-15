package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class InMemoryGroceryStoreRepository implements StoreRepository {

    public InMemoryGroceryStoreRepository(List<GroceryStore> stores) {
        for (GroceryStore store : stores) {
            storesById.put(store.getOutletId(), store);
        }
    }

    @Override
    public Optional<Outlet> findById(String storeId) {
        return Optional.ofNullable(storesById.get(storeId));
    }

    @Override
    public Optional<Outlet> findByName(String storeName) {
        return storesById.values().stream()
                .filter(store -> store.getName().equals(storeName))
                .findFirst();
    }

    @Override
    public void save(Outlet store) {
        storesById.put(store.getOutletId(), store);
    }


    @Override
    public Collection<Outlet> findAll() {
        return Collections.unmodifiableCollection(storesById.values());
    }
}