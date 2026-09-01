CREATE TABLE player_snapshots (
    id BIGSERIAL PRIMARY KEY,

    player_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,

    snapshot_date DATE NOT NULL,
    captured_at TIMESTAMP NOT NULL,

    points INTEGER NOT NULL,
    market_value BIGINT NOT NULL,
    value_fluctuation BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,

    team_id BIGINT,

    owner_id BIGINT,
    purchase_price BIGINT,

    CONSTRAINT fk_player_snapshot_player
        FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_player_snapshot_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_player_snapshot_owner
        FOREIGN KEY (owner_id)
        REFERENCES managers(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_player_snapshot_player_date
        UNIQUE (player_id, snapshot_date)
);

CREATE INDEX idx_player_snapshots_league_date
    ON player_snapshots (league_id, snapshot_date);

CREATE INDEX idx_player_snapshots_player_date
    ON player_snapshots (player_id, snapshot_date);