CREATE TABLE sync_execution_states (
    id BIGSERIAL PRIMARY KEY,
    league_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    last_error TEXT,

    CONSTRAINT uk_sync_execution_states_league
        UNIQUE (league_id)
);