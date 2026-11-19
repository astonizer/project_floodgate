package com.example.floodgate.rules.fanin;

import com.example.floodgate.config.RulesConfig;
import com.example.floodgate.utils.MaskingUtils;
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
                MaskingUtils.maskAccount(beneficiaryAccount),
                windowStartEpochMs,
                windowEndEpochMs,
                count);
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
        if(score == null) return false;

        long scoreMs = score.longValue();
        return windowStartEpochMs <= scoreMs && scoreMs <= windowEndEpochMs;
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
        // Using 2× gives a buffer, entries stay in Redis a bit longer than strictly needed,
        // so out-of-order arrivals still get accurately counted against their true time window.
        long retentionStart = createdAtEpochMs
                - (config.getRules().getFanInWindow().toMillis() * 2);
        zset.removeRangeByScore(key, Double.NEGATIVE_INFINITY, retentionStart - 1);

        redisTemplate.expire(key, config.getRules().getFanInWindow().multipliedBy(2));

        log.debug("FAN_IN recorded src={} → bene={} at={}",
                MaskingUtils.maskAccount(sourceAccount),
                MaskingUtils.maskAccount(beneficiaryAccount),
                createdAtEpochMs);
    }

    private static String key(String beneficiaryAccount) {
        return KEY_PREFIX + beneficiaryAccount;
    }
}

