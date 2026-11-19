package com.example.floodgate.lock;

import java.util.function.Supplier;

public interface DistributedLockService {
    <T> T executeWithLock(String lockKey, Supplier<T> action);
}
