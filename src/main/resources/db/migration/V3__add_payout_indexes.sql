CREATE INDEX idx_payouts_status_created_held
    ON payouts (created_at DESC)
    WHERE status = 'HELD';

CREATE INDEX idx_payouts_status_created_all
    ON payouts (status, created_at DESC);

CREATE INDEX idx_payouts_new_beneficiary
    ON payouts (source_account, beneficiary_account)
    WHERE status IN ('AUTO_RELEASED', 'RELEASED');