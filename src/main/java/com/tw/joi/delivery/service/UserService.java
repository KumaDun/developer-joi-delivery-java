package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.exception.UserDoesNotExistException;
import com.tw.joi.delivery.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User fetchUserById(String userId) {
        Assert.hasText(userId, "User ID must not be blank");
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(userId));
    }

    public void recordLastOrder(String userId, String orderId) {
        Assert.hasText(orderId, "Order ID must not be blank");
        User user = fetchUserById(userId);
        synchronized (user) {
            String previousOrderId = user.getLastOrderId();
            user.setLastOrderId(orderId);
            try {
                userRepository.save(user);
            } catch (RuntimeException exception) {
                user.setLastOrderId(previousOrderId);
                throw exception;
            }
        }
    }

}
