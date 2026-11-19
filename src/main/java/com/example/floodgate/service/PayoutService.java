package com.example.floodgate.service;

import com.example.floodgate.models.dto.PagedPayoutResponse;
import com.example.floodgate.models.dto.PayoutRequest;
import com.example.floodgate.models.dto.PayoutResponse;

public interface PayoutService {
    PayoutResponse process(PayoutRequest request);
    PagedPayoutResponse getHeldPayouts(int page, int size);
}
