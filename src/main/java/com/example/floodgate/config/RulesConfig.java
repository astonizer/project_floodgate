package com.example.floodgate.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "payout")
@Data
public class RulesConfig {

    private Rules rules = new Rules();
    private Lock  lock  = new Lock();

    @Data
    public static class Rules {
        /** HIGH_AMOUNT rule fires when amount >= this. Default: 200,000. */
        private BigDecimal highAmount = new BigDecimal("200000");
        /** NEW_BENEFICIARY rule fires only when amount >= this. Default: 50,000. */
        private BigDecimal newBeneficiaryMinAmount = new BigDecimal("50000");
        /** BENEFICIARY_FAN_IN fires when distinct payers >= this. Default: 5. */
        private int fanInThreshold = 5;
        /** Rolling window for fan-in tracking. Default: 60 minutes. */
        private Duration fanInWindow = Duration.ofMinutes(60);
    }

    @Data
    public static class Lock {
        /** TTL for the distributed Redis lock on a payoutId. Default: 5000 ms. */
        private long ttlMs = 5000;
        /** Retry attempts if lock not immediately acquired. */
        private int retryAttempts = 3;
        /** Delay between retries in milliseconds. */
        private long retryDelayMs = 100;
    }
}
