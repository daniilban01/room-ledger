-- Optional message from the person recording the payment, e.g. "rent for November" or "paid in cash".
-- A new migration instead of editing V4: migrations that already ran must never change,
-- otherwise Flyway's checksum validation fails on every database that applied the old version.
ALTER TABLE settlements ADD COLUMN note VARCHAR(200);
