package com.tw.joi.delivery.dto.response;

import com.tw.joi.delivery.domain.PaymentStatus;

public record PaymentResponse(PaymentStatus status, String message) {
}
