CREATE TABLE matchday_contexts (
    id BIGSERIAL PRIMARY KEY,

    league_id BIGINT NOT NULL,
    biwenger_round_id BIGINT NOT NULL,

    split_round VARCHAR(50),
    lineup_show VARCHAR(50),
    lineup_round_changes INTEGER,
    lineup_round_changes_in VARCHAR(50),
    lineup_round_change_strategy BOOLEAN,

    CONSTRAINT fk_matchday_contexts_league
        FOREIGN KEY (league_id)
        REFERENCES leagues(id),

    CONSTRAINT uk_matchday_context_league_round
        UNIQUE (league_id, biwenger_round_id)
);