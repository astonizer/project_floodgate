package com.example.azentio.floodgate.rules;

import com.example.azentio.floodgate.models.dto.PayoutRequest;
import com.example.azentio.floodgate.models.enums.RuleCode;

import java.util.Optional;

public interface PayoutRule {
    /**
     * @return the rule code if this rule fires, otherwise empty
     */
    Optional<RuleCode> evaluate(PayoutRequest request);
}
