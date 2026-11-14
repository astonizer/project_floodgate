CREATE TYPE payout_status AS ENUM (
    'AUTO_RELEASED',
    'HELD',
    'RELEASED',
    'REJECTED'
);

CREATE TYPE payout_channel AS ENUM (
    'IMPS',
    'NEFT',
    'RTGS'
);