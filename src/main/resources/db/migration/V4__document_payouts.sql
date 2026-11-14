COMMENT ON TABLE payouts IS
    'Central ledger of all submitted payouts with screening decisions.';

COMMENT ON COLUMN payouts.payout_id IS
    'Caller-supplied idempotency key; unique across all rows.';

COMMENT ON COLUMN payouts.source_account IS
    'Originating bank account number; stored in full, masked on output.';

COMMENT ON COLUMN payouts.beneficiary_account IS
    'Destination bank account number; stored in full, masked on output.';

COMMENT ON COLUMN payouts.beneficiary_pan IS
    'Optional PAN of beneficiary; stored in full, masked on output.';

COMMENT ON COLUMN payouts.reasons IS
    'Array of rule codes that fired.';

COMMENT ON COLUMN payouts.created_at IS
    'Event time supplied by caller; used for fan-in rolling window.';

COMMENT ON COLUMN payouts.processed_at IS
    'Wall-clock timestamp at which the service made a decision.';