package com.example.floodgate.exception;

public class LockAcquisitionException extends RuntimeException {
    public LockAcquisitionException(String lockKey, int attempts) {
        super("Failed to acquire lock for key [" + lockKey + "] after " + attempts + " attempt(s)");
    }
}
