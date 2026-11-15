package com.tw.joi.delivery.exception;

public class StoreIdDoesNotExistException extends RuntimeException {
    public StoreIdDoesNotExistException(String outletId) {
        super("Store: " + outletId + " does not exist");
    }
}
