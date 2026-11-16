package com.tw.joi.delivery.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
        @NotNull(message = "Stock adjustment is required")
        Integer delta
) {
}
