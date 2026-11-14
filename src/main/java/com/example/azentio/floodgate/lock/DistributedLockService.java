package com.example.azentio.floodgate.lock;

import java.util.function.Supplier;

public interface DistributedLockService {
    <T> T executeWithLock(String lockKey, Supplier<T> action);
}
