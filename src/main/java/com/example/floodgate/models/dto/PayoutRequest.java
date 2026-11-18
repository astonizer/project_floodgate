package com.example.floodgate.models.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Incoming payout request body.
 */
@Value
@Builder
@JsonDeserialize(builder = PayoutRequest.PayoutRequestBuilder.class)
public class PayoutRequest {

    @NotBlank(message = "payoutId is required")
    @Size(max = 64, message = "payoutId must not exceed 64 characters")
    String payoutId;

    @NotBlank(message = "sourceAccount is required")
    @Size(min = 9, max = 50, message = "sourceAccount must be between 9 and 50 characters")
    String sourceAccount;

    @NotBlank(message = "beneficiaryAccount is required")
    @Size(min = 9, max = 50, message = "beneficiaryAccount must be between 9 and 50 characters")
    String beneficiaryAccount;

    @NotBlank(message = "beneficiaryIfsc is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
            message = "beneficiaryIfsc must be a valid 11-character IFSC code")
    String beneficiaryIfsc;

    /** Optional. Validated only when present. Format: 5 uppercase, 4 digits, 1 uppercase. */
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$",
            message = "beneficiaryPan must be a valid 10-character PAN")
    String beneficiaryPan;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    @Digits(integer = 16, fraction = 2,
            message = "amount must have at most 16 integer digits and 2 decimal places")
    BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    String currency;

    @NotBlank(message = "channel is required")
    @Pattern(regexp = "^(IMPS|NEFT|RTGS)$",
            message = "channel must be one of IMPS, NEFT, RTGS")
    String channel;

    @NotNull(message = "createdAt is required")
    OffsetDateTime createdAt;

    @JsonPOJOBuilder(withPrefix = "")
    public static class PayoutRequestBuilder {}
}
