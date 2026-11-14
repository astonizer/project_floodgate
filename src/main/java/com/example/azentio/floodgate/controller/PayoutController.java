package com.example.azentio.floodgate.controller;

import com.example.azentio.floodgate.models.dto.PagedPayoutResponse;
import com.example.azentio.floodgate.models.dto.PayoutRequest;
import com.example.azentio.floodgate.models.dto.PayoutResponse;
import com.example.azentio.floodgate.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    /**
     * Screen a payout.
     *
     * <p>Idempotent on {@code payoutId}: submitting the same ID twice returns
     * the original decision without re-evaluating rules.
     *
     * @param request validated payout request
     * @return 200 with decision; 400 on validation failure; 409 on duplicate
     *         with differing payload; 503 if lock cannot be acquired
     */
    @PostMapping
    public ResponseEntity<PayoutResponse> screen(@Valid @RequestBody PayoutRequest request) {
        PayoutResponse response = payoutService.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * List held payouts, newest first, with masked account numbers and PANs.
     *
     * @param page zero-based page index (default 0)
     * @param size page size (default 20)
     * @return paginated response
     */
    @GetMapping
    public ResponseEntity<PagedPayoutResponse> getHeld(
            @RequestParam(defaultValue = "HELD") String status,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "20")   int    size) {
        PagedPayoutResponse response = payoutService.getHeldPayouts(page, size);
        return ResponseEntity.ok(response);
    }
}
