CREATE TABLE payouts (
                         id                  BIGSERIAL PRIMARY KEY,
                         payout_id           VARCHAR(64) NOT NULL,
                         source_account      VARCHAR(50) NOT NULL,
                         beneficiary_account VARCHAR(50) NOT NULL,
                         beneficiary_ifsc    VARCHAR(11) NOT NULL,
                         beneficiary_pan     VARCHAR(10),
                         amount              NUMERIC(18, 2) NOT NULL,
                         currency            VARCHAR(3) NOT NULL,
                         channel             payout_channel NOT NULL,
                         status              payout_status NOT NULL,
                         reasons             TEXT[] NOT NULL DEFAULT '{}',
                         created_at          TIMESTAMPTZ NOT NULL,
                         processed_at        TIMESTAMPTZ NOT NULL
);

ALTER TABLE payouts
    ADD CONSTRAINT uq_payouts_payout_id
        UNIQUE (payout_id);