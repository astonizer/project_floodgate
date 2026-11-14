package com.example.azentio.floodgate.service.impl;


import com.example.azentio.floodgate.models.dto.PagedPayoutResponse;
import com.example.azentio.floodgate.models.dto.PayoutRequest;
import com.example.azentio.floodgate.models.dto.PayoutResponse;
import com.example.azentio.floodgate.models.entity.Payout;
import com.example.azentio.floodgate.models.enums.PayoutStatus;
import com.example.azentio.floodgate.models.enums.RuleCode;
import com.example.azentio.floodgate.repository.PayoutRepository;
import com.example.azentio.floodgate.rules.PayoutRule;
import com.example.azentio.floodgate.rules.fanin.FanInStore;
import com.example.azentio.floodgate.service.PayoutService;
import com.example.azentio.floodgate.utils.MaskingUtils;
import lock.PessimisticDbLockService;
import lock.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class PayoutServiceImpl implements PayoutService {

    private final List<PayoutRule> rules;
    private final PayoutRepository repository;
    private final RedisLockService redisLockService;
    private final PessimisticDbLockService dbLockService;
    private final FanInStore fanInStore;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Process a payout and return a screening decision.
     *
     * <p>Idempotent on {@code payoutId}: a second call with the same ID returns
     * the original decision without re-evaluating rules or touching the DB.
     */
    @Transactional
    public PayoutResponse process(PayoutRequest request) {
        log.info("Processing payout payoutId={} src={} bene={} amount={}",
                request.getPayoutId(),
                MaskingUtils.maskAccount(request.getSourceAccount()),
                MaskingUtils.maskAccount(request.getBeneficiaryAccount()),
                request.getAmount());

        // Step 1: Fast optimistic read (no lock)
        Optional<Payout> existing = repository.findByPayoutId(request.getPayoutId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Step 2: Lock → double-check → process
        try {
            return redisLockService.executeWithLock(
                    request.getPayoutId(),
                    () -> processUnderLock(request));
        } catch (Exception redisEx) {
            // Redis may be down — fall back to DB pessimistic lock
            log.warn("Redis lock unavailable for payoutId={}, falling back to DB lock: {}",
                    request.getPayoutId(), redisEx.getMessage());
            return dbLockService.executeWithLock(
                    request.getPayoutId(),
                    () -> processUnderLock(request));
        }
    }

    /**
     * Return paginated HELD payouts, newest first, with masked fields.
     */
    @Transactional(readOnly = true)
    public PagedPayoutResponse getHeldPayouts(int page, int size) {
        Page<Payout> pg = repository.findByStatusOrderByCreatedAtDesc(
                PayoutStatus.HELD,
                PageRequest.of(page, size));

        List<PagedPayoutResponse.PayoutItem> items = pg.getContent().stream()
                .map(this::toPayoutItem)
                .collect(Collectors.toList());

        return PagedPayoutResponse.builder()
                .content(items)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .build();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    /**
     * Must be called while holding a lock on {@code request.payoutId()}.
     */
    private PayoutResponse processUnderLock(PayoutRequest request) {
        // Double-checked read
        Optional<Payout> existing = repository.findByPayoutId(request.getPayoutId());
        if (existing.isPresent()) {
            log.debug("Idempotency replay payoutId={}", request.getPayoutId());
            return toResponse(existing.get());
        }

        // Evaluate all rules; collect every firing code
        List<RuleCode> firingCodes = rules.stream()
                .map(rule -> rule.evaluate(request))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        PayoutStatus status = firingCodes.isEmpty()
                ? PayoutStatus.AUTO_RELEASED
                : PayoutStatus.HELD;

        // Persist
        OffsetDateTime now = OffsetDateTime.now();
        Payout payout = Payout.builder()
                .payoutId(request.getPayoutId())
                .sourceAccount(request.getSourceAccount())
                .beneficiaryAccount(request.getBeneficiaryAccount())
                .beneficiaryIfsc(request.getBeneficiaryIfsc())
                .beneficiaryPan(request.getBeneficiaryPan())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .channel(request.getChannel())
                .status(status)
                .reasons(firingCodes.stream().map(Enum::name).collect(Collectors.toList()))
                .createdAt(request.getCreatedAt())
                .processedAt(now)
                .build();

        repository.save(payout);

        // Record to fan-in store only for auto-released payouts
        if (status == PayoutStatus.AUTO_RELEASED) {
            long createdAtMs = request.getCreatedAt().toInstant().toEpochMilli();
            fanInStore.record(
                    request.getBeneficiaryAccount(),
                    request.getSourceAccount(),
                    createdAtMs);
        }

        log.info("Payout decision payoutId={} status={} reasons={}",
                request.getPayoutId(), status, firingCodes);

        return PayoutResponse.builder()
                .payoutId(request.getPayoutId())
                .status(status)
                .reasons(firingCodes)
                .build();
    }

    private PayoutResponse toResponse(Payout p) {
        List<RuleCode> codes = p.getReasons().stream()
                .map(RuleCode::valueOf)
                .collect(Collectors.toList());
        return PayoutResponse.builder()
                .payoutId(p.getPayoutId())
                .status(p.getStatus())
                .reasons(codes)
                .build();
    }

    private PagedPayoutResponse.PayoutItem toPayoutItem(Payout p) {
        List<RuleCode> codes = p.getReasons().stream()
                .map(RuleCode::valueOf)
                .collect(Collectors.toList());
        return PagedPayoutResponse.PayoutItem.builder()
                .payoutId(p.getPayoutId())
                .sourceAccount(MaskingUtils.maskAccount(p.getSourceAccount()))
                .beneficiaryAccount(MaskingUtils.maskAccount(p.getBeneficiaryAccount()))
                .beneficiaryIfsc(p.getBeneficiaryIfsc())
                .beneficiaryPan(MaskingUtils.maskPan(p.getBeneficiaryPan()))
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .channel(p.getChannel())
                .status(p.getStatus())
                .reasons(codes)
                .createdAt(p.getCreatedAt())
                .processedAt(p.getProcessedAt())
                .build();
    }
}

