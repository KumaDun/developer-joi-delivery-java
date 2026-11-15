package com.tw.joi.delivery.service;

import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

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