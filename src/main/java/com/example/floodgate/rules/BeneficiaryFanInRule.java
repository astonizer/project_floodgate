package com.example.floodgate.rules;

import com.example.floodgate.config.RulesConfig;
import com.example.floodgate.models.dto.PayoutRequest;
import com.example.floodgate.models.enums.RuleCode;
import com.example.floodgate.rules.fanin.FanInStore;
import com.example.floodgate.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeneficiaryFanInRule implements PayoutRule {

    private final RulesConfig config;
    private final FanInStore fanInStore;

    @Override
    public Optional<RuleCode> evaluate(PayoutRequest request) {
        long createdAtMs = request.getCreatedAt().toInstant().toEpochMilli();
        long windowMs = config.getRules().getFanInWindow().toMillis();
        long windowStartMs = createdAtMs - windowMs;
        int threshold = config.getRules().getFanInThreshold();

        // Count existing distinct payers in the window (before this payout is recorded)
        long existingCount = fanInStore.countDistinctPayers(
                request.getBeneficiaryAccount(),
                windowStartMs,
                windowMs
        );

        // Is the current payer already counted in the window?
        // - Yes → total stays at existingCount (repeat payer, counted once)
        // - No  → total = existingCount + 1 (new distinct payer)
        boolean alreadyCounted = fanInStore.isPayerInWindow(
                request.getBeneficiaryAccount(),
                request.getSourceAccount(),
                windowStartMs,
                windowMs
        );

        long projectedTotal = alreadyCounted ? existingCount: existingCount + 1;
        boolean fires = projectedTotal >= threshold;

        log.debug("FAN_IN eval payoutId={} bene={} src={} existing={} alreadyCounted={} projected={} threshold={} fires={}",
                request.getPayoutId(),
                MaskingUtils.maskAccount(request.getBeneficiaryAccount()),
                MaskingUtils.maskAccount(request.getSourceAccount()),
                existingCount, alreadyCounted, projectedTotal, threshold, fires);

        return fires ? Optional.of(RuleCode.BENEFICIARY_FAN_IN) : Optional.empty();
    }
}
