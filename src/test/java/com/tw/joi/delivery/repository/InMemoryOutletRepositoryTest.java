package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import java.util.Collection;
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

    @Test
    void shouldKeepEachRepositoryCatalogIsolated() {
        GroceryStore firstStore = GroceryStore.builder()
                .outletId("store-a").name("Fresh Picks").build();
        StoreRepository first = new InMemoryOutletRepository(List.of(firstStore));
        StoreRepository second = new InMemoryOutletRepository(List.of());

        assertTrue(second.findById("store-a").isEmpty());
        assertTrue(second.findAll().isEmpty());

        GroceryStore otherStore = GroceryStore.builder()
                .outletId("store-a").name("Other Store").build();
        second.save(otherStore);

        assertSame(firstStore, first.findById("store-a").orElseThrow());
        assertSame(otherStore, second.findById("store-a").orElseThrow());
    }

    @Test
    void shouldReturnReadOnlyCatalogSnapshot() {
        GroceryStore firstStore = GroceryStore.builder()
                .outletId("store-a").name("Fresh Picks").build();
        StoreRepository repository = new InMemoryOutletRepository(List.of(firstStore));
        Collection<Outlet<?>> snapshot = repository.findAll();

        repository.save(GroceryStore.builder()
                .outletId("store-b").name("Natural Choice").build());

        assertEquals(List.of(firstStore), snapshot);
        assertEquals(2, repository.findAll().size());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertSame(firstStore, repository.findById("store-a").orElseThrow());
    }
}
