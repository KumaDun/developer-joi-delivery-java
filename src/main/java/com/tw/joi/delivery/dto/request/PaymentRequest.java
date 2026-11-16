package com.tw.joi.delivery.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;

public record PaymentRequest(
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "[0-9]{13,19}", message = "Card number must contain 13 to 19 digits")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String cardNumber,
        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "[0-9]{3,4}", message = "CVV must contain 3 or 4 digits")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String cvv,
        @NotBlank(message = "Expiration date is required")
        @Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])", message = "Expiration date must use YYYY-MM with a valid month")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String expirationDate
) {
    @Override
    public String toString() {
        return "PaymentRequest[redacted]";
    }

    public static final class StrictStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (String) context.handleUnexpectedToken(String.class, parser);
            }
            return parser.getText();
        }
    }
}
