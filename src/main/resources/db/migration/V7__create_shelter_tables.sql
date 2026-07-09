-- feature ⑤（evacuation / EvacuationShelter 集約）の初回スキーマ。#65
-- §7.1 の i18n 分割を踏襲し、言語非依存ベース shelter と
-- 言語別 shelter_localization に分ける。§7.2 の方針に従い、当面は
-- latitude/longitude を素の数値カラムで保持する（PostGIS 化は #11）。
--
-- 本マイグレーション適用後、Hibernate は ddl-auto=validate でこのスキーマを
-- 検証するのみ（スキーマの所有は Flyway）。列の型・長さは domain/evacuation の
-- Value Object の不変条件に合わせる。type は Client のリテラルに一致する
-- 閉じた集合のため CHECK 制約で列挙を固定する。facility_category は
-- ShelterFacilityCategory と同様、運営側で拡張されうるため CHECK では固定しない
-- （spot.genre と同じ扱い）。

CREATE TABLE shelter (
    -- EvacuationShelter.Id。VO の上限長 128 に合わせる
    id                VARCHAR(128)     NOT NULL,
    -- §7.2: 当面は素の数値カラムで保持（最寄り検索が必要になったら PostGIS へ）
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    -- ShelterType（Client リテラル: designated-emergency-evacuation-site /
    -- designated-evacuation-shelter / dual-use）
    type              VARCHAR(64)      NOT NULL,
    -- ShelterFacilityCategory.Code（運営側で拡張されうるため列挙は固定しない）
    facility_category VARCHAR(64)      NOT NULL,
    -- ShelterMedia.imageUrl
    image_url         TEXT             NOT NULL,
    -- ShelterCapacity（任意・正の整数）
    capacity          INTEGER,
    accessible        BOOLEAN          NOT NULL,
    -- ShelterExternalUrl（任意・http/https の絶対 URL）
    external_url      TEXT,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT pk_shelter PRIMARY KEY (id),
    CONSTRAINT ck_shelter_latitude  CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_shelter_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_shelter_type      CHECK (type IN (
        'designated-emergency-evacuation-site',
        'designated-evacuation-shelter',
        'dual-use'
    )),
    CONSTRAINT ck_shelter_capacity  CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE shelter_localization (
    shelter_id VARCHAR(128) NOT NULL,
    -- Language の enum コード（ja / en / ko / zh）
    language   VARCHAR(8)   NOT NULL,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(255) NOT NULL,
    -- §7.1: 1 避難所 × 1 言語で一意
    CONSTRAINT pk_shelter_localization PRIMARY KEY (shelter_id, language),
    -- 避難所削除時はローカライズも連動削除
    CONSTRAINT fk_shelter_localization_shelter FOREIGN KEY (shelter_id)
        REFERENCES shelter (id) ON DELETE CASCADE
);
