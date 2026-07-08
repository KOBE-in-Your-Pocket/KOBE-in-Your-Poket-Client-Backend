-- feature ④（manner / MannerItem 集約）の初回スキーマ。
-- 要件定義 §4.5 の DB スケッチと domain/manner の Value Object に合わせる。
-- §7.1 の i18n 分割を踏襲し、言語非依存ベース manner_item と
-- 言語別 manner_item_localization に分ける。relatedSpotIds は M-2 に従い
-- Tourism の Spot.id への ID 参照のみを manner_item_spot で持ち、FK では結合しない
-- （bounded contexts 間は同一性を「値」として扱う。実在検証はしない）。
--
-- 本マイグレーション適用後、Hibernate は ddl-auto=validate でこのスキーマを
-- 検証するのみ（スキーマの所有は Flyway）。kind / scope は Client のリテラルに
-- 一致する閉じた集合のため CHECK 制約で列挙を固定する。

CREATE TABLE manner_item (
    -- MannerItem.Id（slug 等）。VO の上限長 128 に合わせる
    id         VARCHAR(128) NOT NULL,
    -- MannerIcon（アイコン識別キー。画像 URL ではない。M-3）
    icon       VARCHAR(64)  NOT NULL,
    -- MannerKind（Client リテラル: manner / rule）
    kind       VARCHAR(16)  NOT NULL,
    -- MannerScope（Client リテラル: local / japan）
    scope      VARCHAR(16)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_manner_item PRIMARY KEY (id),
    CONSTRAINT ck_manner_item_kind  CHECK (kind IN ('manner', 'rule')),
    CONSTRAINT ck_manner_item_scope CHECK (scope IN ('local', 'japan'))
);

CREATE TABLE manner_item_localization (
    manner_item_id VARCHAR(128) NOT NULL,
    -- Language の enum コード（ja / en / ko / zh）
    language       VARCHAR(8)   NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    -- §7.1: 1 項目 × 1 言語で一意
    CONSTRAINT pk_manner_item_localization PRIMARY KEY (manner_item_id, language),
    -- 項目削除時はローカライズも連動削除
    CONSTRAINT fk_manner_item_localization_item FOREIGN KEY (manner_item_id)
        REFERENCES manner_item (id) ON DELETE CASCADE
);

CREATE TABLE manner_item_spot (
    manner_item_id VARCHAR(128) NOT NULL,
    -- Tourism の Spot.id への参照（ID 参照のみ。M-2 により spot への FK は張らない）
    spot_id        VARCHAR(128) NOT NULL,
    -- 同一項目 × 同一スポットの重複を禁止
    CONSTRAINT pk_manner_item_spot PRIMARY KEY (manner_item_id, spot_id),
    -- 項目削除時は関連づけも連動削除（spot 側へは張らない）
    CONSTRAINT fk_manner_item_spot_item FOREIGN KEY (manner_item_id)
        REFERENCES manner_item (id) ON DELETE CASCADE
);
