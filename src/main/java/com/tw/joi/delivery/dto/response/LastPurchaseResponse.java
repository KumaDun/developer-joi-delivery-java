package com.tw.joi.delivery.dto.response;

import java.time.Instant;

public record LastPurchaseResponse(
        String orderId,
        String outletId,
        String outletName,
        Instant orderedAt
) {
}
