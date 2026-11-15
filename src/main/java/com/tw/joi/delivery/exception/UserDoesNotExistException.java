package com.tw.joi.delivery.exception;

public class UserDoesNotExistException extends RuntimeException {

    public UserDoesNotExistException(String userId) {
        super("User: " + userId + " does not exist");
    }
}
