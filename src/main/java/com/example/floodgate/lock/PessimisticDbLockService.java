package com.example.floodgate.lock;

import com.example.floodgate.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PessimisticDbLockService implements DistributedLockService {

    private final PayoutRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        log.debug("DB pessimistic lock for payoutId={}", lockKey);
        // Lock the row if it exists; if not, UNIQUE constraint handles INSERT races
        repository.findByPayoutIdForUpdate(lockKey);
        return action.get();
    }
}
