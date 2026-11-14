package com.example.azentio.floodgate.rules.fanin;


import com.example.azentio.floodgate.config.RulesConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFanInStore implements FanInStore {

    private static final String KEY_PREFIX = "fan_in:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RulesConfig config;

    @Override
    public long countDistinctPayers(String beneficiaryAccount,
                                    long windowStartEpochMs,
                                    long windowEndEpochMs) {
        String key = key(beneficiaryAccount);
        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(key, windowStartEpochMs, windowEndEpochMs);
        long count = (members == null) ? 0L : members.size();
        log.debug("FAN_IN count bene={} window=[{},{}] → {}",
                beneficiaryAccount, windowStartEpochMs, windowEndEpochMs, count);
        return count;
    }

    @Override
    public boolean isPayerInWindow(String beneficiaryAccount,
                                   String sourceAccount,
                                   long windowStartEpochMs,
                                   long windowEndEpochMs) {
        String key = key(beneficiaryAccount);
        // ZSCORE returns null if the member doesn't exist
        Double score = redisTemplate.opsForZSet().score(key, sourceAccount);
        if (score == null) return false;
        long scoreMs = score.longValue();
        return scoreMs >= windowStartEpochMs && scoreMs <= windowEndEpochMs;
    }

    @Override
    public void record(String beneficiaryAccount,
                       String sourceAccount,
                       long createdAtEpochMs) {
        String key = key(beneficiaryAccount);
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        // ZADD: inserts or updates score to latest payment timestamp
        zset.add(key, sourceAccount, createdAtEpochMs);

        // Trim entries older than 2× window to handle out-of-order arrivals
        long retentionStart = createdAtEpochMs
                - (config.getRules().getFanInWindow().toMillis() * 2);
        zset.removeRangeByScore(key, Double.NEGATIVE_INFINITY, retentionStart - 1);

        log.debug("FAN_IN recorded src={} → bene={} at={}", sourceAccount, beneficiaryAccount, createdAtEpochMs);
    }

    private static String key(String beneficiaryAccount) {
        return KEY_PREFIX + beneficiaryAccount;
    }
}

