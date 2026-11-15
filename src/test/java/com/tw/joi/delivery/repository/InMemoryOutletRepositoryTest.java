package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryOutletRepositoryTest {

    @Test
    void shouldReturnStoreWhenItExists() {
        GroceryStore store = GroceryStore.builder()
                .outletId("store-a")
                .name("Fresh Picks")
                .build();

        StoreRepository repository =
                new InMemoryOutletRepository(List.of(store));

        Optional<Outlet<?>> result = repository.findById("store-a");

        assertSame(store, result.orElseThrow());
    }

    @Test
    void shouldReturnNullWhenStoreIdDoesNotExist() {
        GroceryStore store = GroceryStore.builder()
                .outletId("store-a")
                .name("Fresh Picks")
                .build();

        StoreRepository repository =
                new InMemoryOutletRepository(List.of(store));

        Optional<Outlet<?>> result = repository.findById("store-b");

        assertTrue(result.isEmpty());

    }

    @Test
    void shouldReturnNullWhenStoreNameDoesNotExist() {
        StoreRepository repository =
                new InMemoryOutletRepository(List.of());
        Optional<Outlet<?>> result = repository.findByName("Fresh Pick");

        assertTrue(result.isEmpty());
    }
}