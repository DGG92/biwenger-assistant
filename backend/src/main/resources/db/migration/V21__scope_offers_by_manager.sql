ALTER TABLE offers
    ADD COLUMN owner_manager_id BIGINT;

ALTER TABLE offers
    ADD CONSTRAINT fk_offers_owner_manager
        FOREIGN KEY (owner_manager_id)
        REFERENCES managers(id);

UPDATE offers
SET owner_manager_id = (
    SELECT m.id
    FROM managers m
    WHERE m.biwenger_manager_id = 11467137
      AND m.league_id = offers.league_id
)
WHERE owner_manager_id IS NULL;

ALTER TABLE offers
    ALTER COLUMN owner_manager_id SET NOT NULL;

ALTER TABLE offers
    DROP CONSTRAINT ukns5tqt7h4tcmeko8g7jlottx2;

ALTER TABLE offers
    ADD CONSTRAINT uk_offers_owner_biwenger_offer
        UNIQUE (owner_manager_id, biwenger_offer_id);

CREATE INDEX idx_offers_owner_manager
    ON offers(owner_manager_id);