package com.tw.joi.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaceOrderRequest(
        @NotBlank(message = "User ID must not be blank")
        String userId
) {
}