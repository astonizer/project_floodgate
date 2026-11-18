package com.example.floodgate.models.dto;

import com.example.floodgate.models.enums.PayoutStatus;
import com.example.floodgate.models.enums.RuleCode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class PagedPayoutResponse {

    List<PayoutItem> content;
    int page;
    int size;
    long totalElements;
    int totalPages;

    @Value
    @Builder
    public static class PayoutItem {
        String payoutId;
        /** Masked — only last 4 digits visible. */
        String sourceAccount;
        /** Masked — only last 4 digits visible. */
        String beneficiaryAccount;
        String beneficiaryIfsc;
        /** Masked — only last 4 chars visible. Null if not provided. */
        String beneficiaryPan;
        BigDecimal amount;
        String currency;
        String channel;
        PayoutStatus status;
        List<RuleCode> reasons;

        OffsetDateTime createdAt;
        OffsetDateTime processedAt;
    }
}
