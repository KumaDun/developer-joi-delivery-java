package com.tw.joi.delivery.repository;

import com.tw.joi.delivery.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void shouldSaveNewUsersAndHistoryWithoutChangingExistingSnapshotsOrOtherRepositories() {
        Collection<User> original = repository.findAll();
        User added = User.builder().userId("user103").lastOrderId("order103").build();

        repository.save(added);

        assertSame(added, repository.findById("user103").orElseThrow());
        assertEquals("order103", repository.findById("user103").orElseThrow().getLastOrderId());
        assertEquals(2, original.size());
        assertEquals(3, repository.findAll().size());
        assertTrue(new InMemoryUserRepository(List.of()).findById("user103").isEmpty());
    }

    @Test
    void shouldRejectNullUser() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        assertEquals(2, repository.findAll().size());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectMissingUserId(String userId) {
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(User.builder().userId(userId).build()));
        assertThrows(IllegalArgumentException.class, () -> repository.findById(userId));
        assertEquals(2, repository.findAll().size());
    }
}
