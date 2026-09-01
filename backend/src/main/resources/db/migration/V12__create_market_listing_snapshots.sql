CREATE TABLE market_listing_snapshots (
    id BIGSERIAL PRIMARY KEY,

    league_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,

    type VARCHAR(50) NOT NULL,

    seller_id BIGINT,

    asking_price BIGINT NOT NULL,
    player_market_value BIGINT NOT NULL,

    published_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,

    extended BOOLEAN NOT NULL,

    last_bid_amount BIGINT,
    last_bid_status VARCHAR(255),
    last_bid_manager_id BIGINT,

    first_captured_at TIMESTAMP NOT NULL,
    last_captured_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_market_listing_snapshot_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_market_listing_snapshot_player
        FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_market_listing_snapshot_seller
        FOREIGN KEY (seller_id)
        REFERENCES managers(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_market_listing_snapshot_last_bid_manager
        FOREIGN KEY (last_bid_manager_id)
        REFERENCES managers(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_market_listing_snapshot_appearance
        UNIQUE (
            league_id,
            player_id,
            type,
            published_at
        )
);

CREATE INDEX idx_market_listing_snapshots_league_published
    ON market_listing_snapshots (
        league_id,
        published_at
    );

CREATE INDEX idx_market_listing_snapshots_player_published
    ON market_listing_snapshots (
        player_id,
        published_at
    );