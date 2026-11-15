package com.tw.joi.delivery.exception;

public class StoreNameDoesNotExistException extends RuntimeException{
    public StoreNameDoesNotExistException(String storeName) {
        super("Store: " + storeName + " does not exist");
    }
}
