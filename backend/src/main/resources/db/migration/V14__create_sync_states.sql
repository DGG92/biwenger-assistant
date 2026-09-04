CREATE TABLE sync_states (
    id BIGSERIAL PRIMARY KEY,
    league_id BIGINT NOT NULL,
    sync_type VARCHAR(50) NOT NULL,
    last_rate_limit_at TIMESTAMP,
    rate_limited_player_id BIGINT,
    retry_after_seconds BIGINT,
    cooldown_until TIMESTAMP,

    CONSTRAINT fk_sync_states_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id),

    CONSTRAINT fk_sync_states_rate_limited_player
        FOREIGN KEY (rate_limited_player_id)
        REFERENCES players(id),

    CONSTRAINT uk_sync_states_league_type
        UNIQUE (league_id, sync_type)
);

CREATE INDEX idx_sync_states_league_type
    ON sync_states (league_id, sync_type);

CREATE INDEX idx_sync_states_cooldown_until
    ON sync_states (cooldown_until);