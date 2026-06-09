-- One-time migration for stock_price redundancy cleanup.
-- Run this after deploying code that reads interval_* columns.

START TRANSACTION;

-- Backfill interval columns from legacy columns where needed.
UPDATE stock_price
SET
    interval_open = COALESCE(interval_open, open_price),
    interval_close = COALESCE(interval_close, price),
    interval_volume = COALESCE(interval_volume, volume),
    interval_high = COALESCE(
        interval_high,
        GREATEST(COALESCE(interval_open, open_price), COALESCE(interval_close, price))
    ),
    interval_low = COALESCE(
        interval_low,
        LEAST(COALESCE(interval_open, open_price), COALESCE(interval_close, price))
    );

-- Drop redundant legacy columns.
ALTER TABLE stock_price
    DROP COLUMN price,
    DROP COLUMN open_price,
    DROP COLUMN volume;

COMMIT;
