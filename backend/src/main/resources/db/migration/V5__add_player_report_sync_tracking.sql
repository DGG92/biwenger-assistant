ALTER TABLE players
    ADD COLUMN reports_last_sync_attempt_at TIMESTAMP,
    ADD COLUMN reports_last_sync_success_at TIMESTAMP;