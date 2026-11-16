package com.tw.joi.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCartItemQuantityRequest(
        @NotBlank(message = "User ID must not be blank") String userId,
        @NotNull(message = "Quantity is required")
        @PositiveOrZero(message = "Quantity must not be negative") Integer quantity
) {
}
