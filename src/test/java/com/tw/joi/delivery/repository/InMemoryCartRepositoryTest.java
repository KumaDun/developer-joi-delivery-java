package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCartRepositoryTest {

    @Test
    void shouldCreateEmptyCartForItsOwnerAndReturnSameCartOnLaterLookups() {
        CartRepository repository = new InMemoryCartRepository();
        User user = user("user101");

        Cart first = repository.getOrCreateForUser(user);
        Cart second = repository.getOrCreateForUser(user("user101"));

        assertSame(first, second);
        assertSame(user, first.getUser());
        assertNotNull(first.getCartId());
        assertFalse(first.getCartId().isBlank());
        assertEquals(first.getCartId(), second.getCartId());
        assertTrue(first.getCartItems().isEmpty());
        assertNull(first.getOutlet());
    }

    @Test
    void shouldGiveDifferentUsersIndependentCarts() {
        CartRepository repository = new InMemoryCartRepository();
        User john = user("user101");
        User rachel = user("user102");

        Cart first = repository.getOrCreateForUser(john);
        Cart second = repository.getOrCreateForUser(rachel);

        assertNotSame(first, second);
        assertNotEquals(first.getCartId(), second.getCartId());
        assertSame(john, first.getUser());
        assertSame(rachel, second.getUser());
        assertNotSame(first.getCartItems(), second.getCartItems());
    }

    @Test
    void shouldKeepCartRepositoriesIndependent() {
        User user = user("user101");

        Cart first = new InMemoryCartRepository().getOrCreateForUser(user);
        Cart second = new InMemoryCartRepository().getOrCreateForUser(user);

        assertNotSame(first, second);
        assertNotEquals(first.getCartId(), second.getCartId());
    }

    @Test
    void shouldCreateOnlyOneCartForConcurrentFirstLookups() throws Exception {
        CartRepository repository = new InMemoryCartRepository();
        User user = user("user101");
        int callers = 8;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        List<Future<Cart>> results = new ArrayList<>();

        try {
            for (int i = 0; i < callers; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Concurrent cart lookup did not start");
                    }
                    return repository.getOrCreateForUser(user);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Cart callers did not become ready");
            start.countDown();

            Cart first = results.getFirst().get(5, TimeUnit.SECONDS);
            for (Future<Cart> result : results) {
                assertSame(first, result.get(5, TimeUnit.SECONDS));
            }
            assertSame(first, repository.getOrCreateForUser(user));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldRejectNullUser() {
        CartRepository repository = new InMemoryCartRepository();

        assertThrows(IllegalArgumentException.class,
                () -> repository.getOrCreateForUser(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldRejectMissingUserId(String userId) {
        CartRepository repository = new InMemoryCartRepository();

        assertThrows(IllegalArgumentException.class,
                () -> repository.getOrCreateForUser(user(userId)));
    }

    private User user(String userId) {
        return User.builder().userId(userId).build();
    }
}
