ALTER TABLE payouts ALTER COLUMN channel TYPE VARCHAR(10) USING channel::text;
DROP TYPE IF EXISTS payout_channel;