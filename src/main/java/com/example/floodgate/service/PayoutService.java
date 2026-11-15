package com.example.floodgate.service;

import com.example.floodgate.models.dto.PagedPayoutResponse;
import com.example.floodgate.models.dto.PayoutRequest;
import com.example.floodgate.models.dto.PayoutResponse;

public interface PayoutService {
    public PayoutResponse process(PayoutRequest request);
    public PagedPayoutResponse getHeldPayouts(int page, int size);
}
