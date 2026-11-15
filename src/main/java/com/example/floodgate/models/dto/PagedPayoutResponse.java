package com.example.floodgate.models.dto;

import com.example.floodgate.models.enums.PayoutStatus;
import com.example.floodgate.models.enums.RuleCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
@Jacksonized
public class PagedPayoutResponse {

    List<PayoutItem> content;
    int              page;
    int              size;
    long             totalElements;
    int              totalPages;

    @Value
    @Builder
    @Jacksonized
    public static class PayoutItem {
        String         payoutId;
        /** Masked — only last 4 digits visible. */
        String         sourceAccount;
        /** Masked — only last 4 digits visible. */
        String         beneficiaryAccount;
        String         beneficiaryIfsc;
        /** Masked — only last 4 chars visible. Null if not provided. */
        String         beneficiaryPan;
        BigDecimal     amount;
        String         currency;
        String         channel;
        PayoutStatus status;
        List<RuleCode> reasons;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime processedAt;
    }
}
