package com.example.floodgate.rules;

import com.example.floodgate.config.RulesConfig;
import com.example.floodgate.models.dto.PayoutRequest;
import com.example.floodgate.models.enums.PayoutStatus;
import com.example.floodgate.models.enums.RuleCode;
import com.example.floodgate.repository.PayoutRepository;
import com.example.floodgate.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewBeneficiaryRule implements PayoutRule {

    private final List<PayoutStatus> CLEAN_STATUSES =
            List.of(PayoutStatus.AUTO_RELEASED, PayoutStatus.RELEASED);

    private final RulesConfig config;
    private final PayoutRepository repository;

    @Override
    public Optional<RuleCode> evaluate(PayoutRequest request) {
        // Amount gate first — avoids DB round-trip for small amounts
        if(request.getAmount().compareTo(config.getRules().getNewBeneficiaryMinAmount()) >= 0) {
            return Optional.of(RuleCode.NEW_BENEFICIARY);
        }

        boolean exists = repository.existsCleanPayout(
                request.getSourceAccount(),
                request.getBeneficiaryAccount(),
                CLEAN_STATUSES
        );

        if(!exists) {
            // MASKING: account numbers must be masked before logging
            log.debug("NEW_BENEFICIARY fired payoutId={} src={} bene={}",
                    request.getPayoutId(),
                    MaskingUtils.maskAccount(request.getSourceAccount()),
                    MaskingUtils.maskAccount(request.getBeneficiaryAccount()));
            return Optional.of(RuleCode.NEW_BENEFICIARY);
        }

        return Optional.empty();
    }
}
