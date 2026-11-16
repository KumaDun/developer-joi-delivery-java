package com.tw.joi.delivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PaymentIntegrationTest.TestClockConfiguration.class)
class PaymentIntegrationTest {

    private static final String UNREADABLE_MESSAGE =
            "Invalid payment request. Supply cardNumber, cvv and expirationDate (YYYY-MM) as strings.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @CsvSource({
            "4111111111111, 007, 2026-11, ACCEPTED, Mock payment accepted.",
            "4111111111111111111, 0007, 2027-01, ACCEPTED, Mock payment accepted.",
            "4111111111110000, 123, 2027-01, REJECTED, Payment declined. Try another card.",
            "4111111111111111, 123, 2026-10, REJECTED, Card has expired."
    })
    void shouldReturnOnlyDecisionAndMessage(
            String cardNumber, String cvv, String expirationDate, String expectedStatus, String message)
            throws Exception {
        String response = pay(objectMapper.writeValueAsString(Map.of(
                        "cardNumber", cardNumber, "cvv", cvv, "expirationDate", expirationDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.message").value(message))
                .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        assertEquals(2, body.size());
        assertFalse(response.contains(cardNumber));
        assertFalse(response.contains(cvv));
        assertFalse(response.contains(expirationDate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"cardNumber\":null,\"cvv\":null,\"expirationDate\":null}"})
    void shouldRejectMissingPaymentFields(String body) throws Exception {
        pay(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("required")));
    }

    @ParameterizedTest
    @CsvSource({
            "cardNumber, 411111111111, Card number must contain 13 to 19 digits",
            "cardNumber, 41111111111111111111, Card number must contain 13 to 19 digits",
            "cardNumber, ４１１１１１１１１１１１１, Card number must contain 13 to 19 digits",
            "cvv, 07, CVV must contain 3 or 4 digits",
            "cvv, 00123, CVV must contain 3 or 4 digits",
            "cvv, １２３, CVV must contain 3 or 4 digits",
            "expirationDate, 2026-00, Expiration date must use YYYY-MM with a valid month",
            "expirationDate, 2026-13, Expiration date must use YYYY-MM with a valid month",
            "expirationDate, 11/2026, Expiration date must use YYYY-MM with a valid month"
    })
    void shouldRejectInvalidPaymentField(String field, String value, String expectedMessage) throws Exception {
        Map<String, Object> body = validPayment();
        body.put(field, value);

        pay(objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cardNumber", "cvv", "expirationDate"})
    void shouldRejectNumericTokensInsteadOfCoercingThemToStrings(String field) throws Exception {
        Map<String, Object> body = validPayment();
        body.put(field, switch (field) {
            case "cardNumber" -> 4111111111111111L;
            case "cvv" -> 123;
            default -> 202701;
        });

        pay(objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UNREADABLE_MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "[]"})
    void shouldReturnSafeErrorForMissingOrMalformedJson(String body) throws Exception {
        pay(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UNREADABLE_MESSAGE));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldNotEchoOrLogRejectedValues(CapturedOutput output) throws Exception {
        String sentinel = "SENSITIVE_PAYMENT_SENTINEL_73912";
        for (String body : new String[]{
                "{\"cardNumber\":\"%s\",\"cvv\":\"%s\",\"expirationDate\":\"2026-13\"}"
                        .formatted(sentinel, sentinel),
                "{\"cardNumber\":\"%s\",".formatted(sentinel)
        }) {
            String response = pay(body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isString())
                    .andReturn().getResponse().getContentAsString();
            assertEquals(1, objectMapper.readTree(response).size());
            assertFalse(response.contains(sentinel));
        }
        assertFalse(output.getAll().contains(sentinel));
    }

    private Map<String, Object> validPayment() {
        return new HashMap<>(Map.of("cardNumber", "4111111111111111",
                "cvv", "007", "expirationDate", "2027-01"));
    }

    private ResultActions pay(String body) throws Exception {
        return mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {
        @Bean
        @Primary
        Clock paymentTestClock() {
            return Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
