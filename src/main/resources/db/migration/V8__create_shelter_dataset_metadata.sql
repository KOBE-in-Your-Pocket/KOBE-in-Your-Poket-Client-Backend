-- #66: 避難所データの出典（source）・データ基準日（as_of）を保持するテーブル。
--
-- 出典は神戸市オープンデータ単一ソースの前提（複数ソースをブレンドする計画は
-- 現時点で無い）のため、避難所 1 件ごとではなく「データセット全体で 1 つ」の
-- meta として持つ（一覧 API のレスポンス meta で返す想定。#67 で配線）。
-- シングルトン行であることを id を固定値 1 に絞る CHECK 制約で保証する。

CREATE TABLE shelter_dataset_metadata (
    id         SMALLINT    NOT NULL,
    -- データの出典（表記名・URL・ライセンスを含む一文。README のライセンス表記と対応）
    source     TEXT        NOT NULL,
    -- データ基準日（出典データセットの最終更新日）
    as_of      DATE        NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_shelter_dataset_metadata PRIMARY KEY (id),
    CONSTRAINT ck_shelter_dataset_metadata_singleton CHECK (id = 1)
);
