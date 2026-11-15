package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOrderStatusRequest(
        @NotNull(message = "Order status must not be null")
        OrderStatus status
) {
}