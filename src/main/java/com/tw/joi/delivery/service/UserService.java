package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User fetchUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(userId));
    }

}
