CREATE TABLE team_standing_snapshots (
    id BIGSERIAL PRIMARY KEY,
    league_id BIGINT NOT NULL,
    biwenger_round_id BIGINT NOT NULL,
    biwenger_team_id BIGINT NOT NULL,
    team_name VARCHAR(255) NOT NULL,
    position INTEGER,
    points INTEGER,
    won INTEGER,
    lost INTEGER,
    tied INTEGER,
    scored INTEGER,
    against INTEGER,

    CONSTRAINT fk_team_standing_snapshot_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id),

    CONSTRAINT uk_team_standing_snapshot_league_round_team
        UNIQUE (
            league_id,
            biwenger_round_id,
            biwenger_team_id
        )
);