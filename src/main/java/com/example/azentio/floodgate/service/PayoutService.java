package com.example.azentio.floodgate.service;

import com.example.azentio.floodgate.models.dto.PagedPayoutResponse;
import com.example.azentio.floodgate.models.dto.PayoutRequest;
import com.example.azentio.floodgate.models.dto.PayoutResponse;

public interface PayoutService {
    public PayoutResponse process(PayoutRequest request);
    public PagedPayoutResponse getHeldPayouts(int page, int size);
}
