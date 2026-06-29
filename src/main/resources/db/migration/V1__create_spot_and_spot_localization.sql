-- feature ①（tourism / Spot）の初回スキーマ。
-- アーキテクチャ §7.1 の i18n 分割を踏襲し、言語非依存ベース spot と
-- 言語別 spot_localization に分ける。businessHours はモックで言語側のため
-- ローカライズ側に置く。
--
-- 本マイグレーション適用後、Hibernate は ddl-auto=validate でこのスキーマを
-- 検証するのみ（スキーマの所有は Flyway）。列の型・長さは domain/tourism の
-- Value Object の不変条件に合わせており、#19 の JPA エンティティはこの定義に
-- マッピングする。

CREATE TABLE spot (
    -- SpotId（slug 等）。VO の上限長 128 に合わせる
    id           VARCHAR(128)     NOT NULL,
    -- Genre は運営側で拡張されうる文字列（固定列挙ではない）
    genre        VARCHAR(64)      NOT NULL,
    -- §7.2: 当面は素の数値カラムで保持（最寄り検索が必要になったら PostGIS へ）
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    image_url    TEXT             NOT NULL,
    -- §7.3: rating はレビュー（feature ②）由来の派生値。feature ① では NULL
    rating_value DOUBLE PRECISION,
    created_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT pk_spot PRIMARY KEY (id),
    CONSTRAINT ck_spot_latitude  CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_spot_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_spot_rating    CHECK (rating_value IS NULL OR rating_value BETWEEN 0 AND 5)
);

CREATE TABLE spot_localization (
    spot_id        VARCHAR(128) NOT NULL,
    -- Language の enum コード（ja / en / ko / zh）
    language       VARCHAR(8)   NOT NULL,
    name           VARCHAR(255) NOT NULL,
    category_label VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    business_hours VARCHAR(255) NOT NULL,
    -- §7.1: 1 スポット × 1 言語で一意
    CONSTRAINT pk_spot_localization PRIMARY KEY (spot_id, language),
    -- スポット削除時はローカライズも連動削除
    CONSTRAINT fk_spot_localization_spot FOREIGN KEY (spot_id)
        REFERENCES spot (id) ON DELETE CASCADE
);
