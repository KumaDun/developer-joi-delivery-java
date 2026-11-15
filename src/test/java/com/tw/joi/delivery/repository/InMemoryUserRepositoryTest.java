package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {

    private User john;
    private User rachel;
    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        john = User.builder()
                .userId("user101")
                .firstName("John")
                .build();

        rachel = User.builder()
                .userId("user102")
                .firstName("Rachel")
                .build();

        repository = new InMemoryUserRepository(
                List.of(john, rachel)
        );
    }

    @Test
    void shouldFindUserById() {
        Optional<User> result = repository.findById("user102");

        assertTrue(result.isPresent());
        assertEquals("user102", result.get().getUserId());
        assertEquals("Rachel", result.get().getFirstName());
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> result = repository.findById("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnAllUsers() {
        Collection<User> result = repository.findAll();

        assertEquals(2, result.size());
        assertTrue(result.contains(john));
        assertTrue(result.contains(rachel));
    }

    @Test
    void shouldReturnEmptyResultsWhenRepositoryIsEmpty() {
        repository = new InMemoryUserRepository(List.of());

        assertTrue(repository.findById("user101").isEmpty());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldPreventModificationOfReturnedCollection() {
        Collection<User> result = repository.findAll();

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.clear()
        );

        assertEquals(2, repository.findAll().size());
    }
}