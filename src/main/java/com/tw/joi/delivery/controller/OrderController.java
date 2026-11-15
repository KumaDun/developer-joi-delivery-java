package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.dto.request.ChangeOrderStatusRequest;
import com.tw.joi.delivery.dto.request.PlaceOrderRequest;
import com.tw.joi.delivery.exception.OrderDoesNotExistException;
import com.tw.joi.delivery.service.OrderService;
import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "User ID is required"
            );
        }

        Order order = orderService.placeOrder(request.userId());

        return ResponseEntity
                .created(URI.create("/orders/" + order.getOrderId()))
                .body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @PathVariable("orderId") String orderId
    ) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Order> changeStatus(
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody ChangeOrderStatusRequest request
    ) {
        if (request.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status is required"
            );
        }

        Order order = orderService.changeStatus(
                orderId, request.status()
        );

        return ResponseEntity.ok(order);
    }

    @ExceptionHandler(OrderDoesNotExistException.class)
    public ResponseEntity<Void> handleOrderNotFound(
            OrderDoesNotExistException exception
    ) {
        return ResponseEntity.notFound().build();
    }
}