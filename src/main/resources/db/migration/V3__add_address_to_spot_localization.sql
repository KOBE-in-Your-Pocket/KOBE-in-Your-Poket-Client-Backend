-- #61: Client の Spot 契約に address（言語別の住所表記）が追加されたため追従する。
-- 既存行を壊さないよう一旦 DEFAULT '' を付けて NOT NULL 化する。

ALTER TABLE spot_localization
    ADD COLUMN address VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE spot_localization
    ALTER COLUMN address DROP DEFAULT;
