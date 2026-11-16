package com.tw.joi.delivery.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SetStockRequest(
        @NotNull(message = "Available stock is required")
        @PositiveOrZero(message = "Available stock must not be negative")
        Integer availableStock
) {
}
