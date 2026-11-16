package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.PaymentStatus;
import com.tw.joi.delivery.dto.request.PaymentRequest;
import com.tw.joi.delivery.dto.response.PaymentResponse;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentService {

    private final Clock clock;

    public MockPaymentService(Clock clock) {
        this.clock = clock;
    }

    public PaymentResponse pay(PaymentRequest request) {
        YearMonth expiration = YearMonth.parse(request.expirationDate());
        if (expiration.isBefore(YearMonth.now(clock))) {
            return new PaymentResponse(PaymentStatus.REJECTED, "Card has expired.");
        }
        if (request.cardNumber().endsWith("0000")) {
            return new PaymentResponse(PaymentStatus.REJECTED, "Payment declined. Try another card.");
        }
        return new PaymentResponse(PaymentStatus.ACCEPTED, "Mock payment accepted.");
    }
}
