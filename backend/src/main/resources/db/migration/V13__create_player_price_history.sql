CREATE TABLE player_price_history (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    price_date DATE NOT NULL,
    market_value BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL,
    captured_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_player_price_history_player
        FOREIGN KEY (player_id)
        REFERENCES players(id),

    CONSTRAINT fk_player_price_history_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id),

    CONSTRAINT uk_player_price_history_player_date
        UNIQUE (player_id, price_date)
);

CREATE INDEX idx_player_price_history_player_date
    ON player_price_history (player_id, price_date);

CREATE INDEX idx_player_price_history_league_date
    ON player_price_history (league_id, price_date);