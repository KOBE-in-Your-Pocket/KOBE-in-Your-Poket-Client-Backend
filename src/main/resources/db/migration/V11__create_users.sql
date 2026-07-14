-- User コンテキストのプロフィールテーブル（#88）。
-- id は Supabase Auth の user id（UUID / JWT sub）と同一。
-- クレデンシャル・ロールは持たない（Auth / app_metadata が正）。

CREATE TABLE users (
    id         UUID         NOT NULL,
    -- User.name / PublicUser.name（最大 100 文字）
    name       VARCHAR(100) NOT NULL,
    -- iconUrl（未設定時は空文字）
    icon_url   TEXT         NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_name_length CHECK (char_length(name) <= 100),
    CONSTRAINT ck_users_name_not_blank CHECK (char_length(trim(name)) > 0)
);
