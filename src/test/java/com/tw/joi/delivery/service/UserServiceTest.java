package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.repository.InMemoryUserRepository;
import com.tw.joi.delivery.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void shouldRecordLatestOrderThroughRepository() {
        User user = User.builder().userId("user101").build();
        UserRepository repository = new InMemoryUserRepository(List.of(user));
        UserService service = new UserService(repository);

        service.recordLastOrder("user101", "order-first");
        service.recordLastOrder("user101", "order-latest");

        assertEquals("order-latest", repository.findById("user101").orElseThrow().getLastOrderId());
    }

    @Test
    void shouldRestorePreviousOrderWhenUserSaveFails() {
        User user = User.builder().userId("user101").lastOrderId("previous-order").build();
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById("user101")).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Storage unavailable")).when(repository).save(user);

        assertThrows(IllegalStateException.class,
                () -> new UserService(repository).recordLastOrder("user101", "new-order"));

        assertEquals("previous-order", user.getLastOrderId());
    }

    @Test
    void shouldRejectBlankOrderIdBeforeAccessingRepository() {
        UserRepository repository = mock(UserRepository.class);

        assertThrows(IllegalArgumentException.class,
                () -> new UserService(repository).recordLastOrder("user101", " "));

        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(userRepository);

        when(userRepository.findById("missing"))
                .thenReturn(Optional.empty());

        UserDoesNotExistException exception = assertThrows(
                UserDoesNotExistException.class,
                () -> userService.fetchUserById("missing")
        );

        assertEquals(
                "User: missing does not exist",
                exception.getMessage()
        );
    }
}
