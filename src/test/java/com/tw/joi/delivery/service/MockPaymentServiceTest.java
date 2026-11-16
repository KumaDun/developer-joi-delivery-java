package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.PaymentStatus;
import com.tw.joi.delivery.dto.request.PaymentRequest;
import com.tw.joi.delivery.dto.response.PaymentResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockPaymentServiceTest {

    private static final String ACCEPTED_CARD = "4111111111111111";
    private static final String DECLINED_CARD = "4111111111110000";

    @ParameterizedTest
    @ValueSource(strings = {"2026-11", "2027-01"})
    void shouldAcceptNonDeclinedCardWithCurrentOrFutureExpiration(String expirationDate) {
        PaymentResponse response = serviceAt("2026-11-16T10:00:00Z")
                .pay(request(ACCEPTED_CARD, expirationDate));

        assertEquals(PaymentStatus.ACCEPTED, response.status());
        assertEquals("Mock payment accepted.", response.message());
    }

    @Test
    void shouldRejectUnexpiredCardWithMockDeclineSuffix() {
        PaymentResponse response = serviceAt("2026-11-16T10:00:00Z")
                .pay(request(DECLINED_CARD, "2027-01"));

        assertEquals(PaymentStatus.REJECTED, response.status());
        assertEquals("Payment declined. Try another card.", response.message());
    }

    @Test
    void shouldRejectExpiredCardBeforeApplyingMockDeclineRule() {
        PaymentResponse response = serviceAt("2026-11-16T10:00:00Z")
                .pay(request(DECLINED_CARD, "2026-10"));

        assertEquals(PaymentStatus.REJECTED, response.status());
        assertEquals("Card has expired.", response.message());
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-30T23:59:59Z, ACCEPTED, Mock payment accepted.",
            "2026-12-01T00:00:00Z, REJECTED, Card has expired."
    })
    void shouldKeepCardValidThroughExpirationMonthAndRejectItAtNextMonth(
            String instant, PaymentStatus expectedStatus, String expectedMessage) {
        PaymentResponse response = serviceAt(instant).pay(request(ACCEPTED_CARD, "2026-11"));

        assertEquals(expectedStatus, response.status());
        assertEquals(expectedMessage, response.message());
    }

    private MockPaymentService serviceAt(String instant) {
        return new MockPaymentService(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private PaymentRequest request(String cardNumber, String expirationDate) {
        return new PaymentRequest(cardNumber, "123", expirationDate);
    }
}
