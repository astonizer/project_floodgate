package com.example.azentio.floodgate.lock;

import com.example.azentio.floodgate.config.RulesConfig;
import com.example.azentio.floodgate.exception.LockAcquisitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService implements DistributedLockService {

    /** Lua script: delete the key only if the value matches our UUID. */
    private static final String RELEASE_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    private static final DefaultRedisScript<Long> RELEASE =
            new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);

    private static final String LOCK_PREFIX = "payout_lock:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RulesConfig config;

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        String key   = LOCK_PREFIX + lockKey;
        String token = UUID.randomUUID().toString();

        int  attempts = config.getLock().getRetryAttempts();
        long delayMs  = config.getLock().getRetryDelayMs();
        long ttlMs    = config.getLock().getTtlMs();

        for (int attempt = 1; attempt <= attempts; attempt++) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, token, Duration.ofMillis(ttlMs));

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Redis lock acquired key={} attempt={}", key, attempt);
                try {
                    return action.get();
                } finally {
                    release(key, token);
                }
            }

            log.debug("Redis lock contended key={} attempt={}/{}", key, attempt, attempts);
            if (attempt < attempts) {
                sleep(delayMs);
            }
        }

        throw new LockAcquisitionException(lockKey, attempts);
    }

    private void release(String key, String token) {
        try {
            Long result = redisTemplate.execute(RELEASE, List.of(key), token);
            if (result == null || result == 0L) {
                log.warn("Redis lock token mismatch or already expired key={}", key);
            }
        } catch (Exception ex) {
            log.warn("Redis lock release failed key={}: {}", key, ex.getMessage());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
