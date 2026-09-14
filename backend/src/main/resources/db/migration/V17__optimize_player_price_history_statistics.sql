CREATE INDEX idx_player_price_history_league_player_date
    ON player_price_history (
        league_id,
        player_id,
        price_date DESC
    );