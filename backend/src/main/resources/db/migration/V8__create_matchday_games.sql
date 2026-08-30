CREATE TABLE matchday_games (
    id BIGSERIAL PRIMARY KEY,

    league_id BIGINT NOT NULL,

    biwenger_round_id BIGINT NOT NULL,
    biwenger_game_id BIGINT NOT NULL,

    round_part INTEGER,
    game_date BIGINT,
    status VARCHAR(50),

    home_team_id BIGINT,
    home_team_name VARCHAR(255),

    away_team_id BIGINT,
    away_team_name VARCHAR(255),

    CONSTRAINT fk_matchday_games_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id),

    CONSTRAINT uk_matchday_game_league_game
        UNIQUE (league_id, biwenger_game_id)
);

CREATE INDEX idx_matchday_games_league_round
    ON matchday_games (
        league_id,
        biwenger_round_id
    );

CREATE INDEX idx_matchday_games_home_team
    ON matchday_games (
        league_id,
        home_team_id
    );

CREATE INDEX idx_matchday_games_away_team
    ON matchday_games (
        league_id,
        away_team_id
    );