package com.example.floodgate.rules;

import com.example.floodgate.config.RulesConfig;
import com.example.floodgate.models.dto.PayoutRequest;
import com.example.floodgate.models.enums.RuleCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * HIGH_AMOUNT rule: fires when {@code amount >= payout.rules.high-amount} (default 200,000).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighAmountRule implements PayoutRule {

    private final RulesConfig config;

    @Override
    public Optional<RuleCode> evaluate(PayoutRequest request) {
        boolean fires = request.getAmount().compareTo(config.getRules().getHighAmount()) >= 0;

        if(fires) {
            // amount is not sensitive — safe to log directly
            log.debug("HIGH_AMOUNT fired payoutId={} amount={} threshold={}",
                    request.getPayoutId(), request.getAmount(),
                    config.getRules().getHighAmount());
            return Optional.of(RuleCode.HIGH_AMOUNT);
        }

        return Optional.empty();
    }
}
