ALTER TABLE movements
DROP CONSTRAINT IF EXISTS movements_type_check;

ALTER TABLE movements
ADD CONSTRAINT movements_type_check
CHECK (
    type IN (
        'MARKET_PURCHASE',
        'MARKET_SALE',
        'AUCTION_PURCHASE',
        'IMMEDIATE_SALE',
        'TRANSFER',
        'LOAN'
    )
);

UPDATE movements
SET
    type = 'MARKET_SALE',
    external_key = REPLACE(
        external_key,
        '|TRANSFER|',
        '|MARKET_SALE|'
    )
WHERE type = 'TRANSFER'
  AND from_manager_id IS NOT NULL
  AND to_manager_id IS NULL;