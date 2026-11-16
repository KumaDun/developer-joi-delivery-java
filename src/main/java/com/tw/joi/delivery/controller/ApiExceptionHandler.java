package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.exception.*;
import com.tw.joi.delivery.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({
            UserDoesNotExistException.class,
            ProductDoesNotExistException.class,
            StoreIdDoesNotExistException.class,
            StoreNameDoesNotExistException.class,
            OrderDoesNotExistException.class
    })
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }
}
