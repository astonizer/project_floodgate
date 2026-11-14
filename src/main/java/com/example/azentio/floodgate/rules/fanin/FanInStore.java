package com.example.azentio.floodgate.rules.fanin;


public interface FanInStore {

    /**
     * Counts the number of distinct source accounts that have paid the given
     * beneficiary within [{@code windowStartEpochMs}, {@code windowEndEpochMs}] inclusive.
     *
     * <p>If the same source account has paid multiple times, it is counted once
     * (using its most recent payment time to determine window eligibility).
     */
    long countDistinctPayers(String beneficiaryAccount,
                             long   windowStartEpochMs,
                             long   windowEndEpochMs);

    /**
     * Returns {@code true} if {@code sourceAccount} has an entry for this beneficiary
     * with a score (last-payment timestamp) within the specified window.
     *
     * <p>Used by the rule to determine whether the current payer is already counted,
     * preventing double-counting of a repeat payer.
     */
    boolean isPayerInWindow(String beneficiaryAccount,
                            String sourceAccount,
                            long   windowStartEpochMs,
                            long   windowEndEpochMs);

    /**
     * Records a payment from {@code sourceAccount} to {@code beneficiaryAccount}
     * at the given event time.
     *
     * <p>If the same source account already exists, its timestamp is updated to
     * {@code createdAtEpochMs} (most recent payment wins). Old entries outside
     * the retention window are pruned.
     */
    void record(String beneficiaryAccount,
                String sourceAccount,
                long   createdAtEpochMs);
}
