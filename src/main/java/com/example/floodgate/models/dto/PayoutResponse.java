package com.example.floodgate.models.dto;

import com.example.floodgate.models.enums.PayoutStatus;
import com.example.floodgate.models.enums.RuleCode;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PayoutResponse {
    String payoutId;
    PayoutStatus status;
    List<RuleCode> reasons;
}

