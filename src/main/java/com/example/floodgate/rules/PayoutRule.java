package com.example.floodgate.rules;

import com.example.floodgate.models.dto.PayoutRequest;
import com.example.floodgate.models.enums.RuleCode;

import java.util.Optional;

public interface PayoutRule {
    /**
     * @return the rule code if this rule fires, otherwise empty
     */
    Optional<RuleCode> evaluate(PayoutRequest request);
}
