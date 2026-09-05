-- #153: ジャンルマスタ。
--
-- これまでジャンルは spot.genre の文字列だけで、マスタも表示名も存在しなかった。
-- そのため運営がジャンルを追加しても、ADMIN（GENRE_LABELS）と Client（i18n）の
-- どちらにもラベルが無く表示できない。マスタ側で表示名を持たせて解決する。
--
-- code は spot.genre と突き合わせる識別子。運営には入力させず、英語ラベルから
-- 自動生成する（application 層の GenreCode.fromLabel）。既存スポットが参照するため
-- 作成後は変更しない（更新 API も code を書き換えない）。

CREATE TABLE genre (
    -- Genre VO（spot.genre）と同じ上限長に合わせる
    code          VARCHAR(64)  NOT NULL,
    -- Client のジャンルフィルタの並び順。運営が制御できるようにする
    display_order INTEGER      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_genre PRIMARY KEY (code),
    -- 自動生成の slug が満たすべき形。手で INSERT するときの事故も防ぐ
    CONSTRAINT ck_genre_code CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_genre_display_order CHECK (display_order >= 0)
);

-- 並び順は一覧のたびに使う。code は PK なので第 2 キーの索引は不要
CREATE INDEX idx_genre_display_order ON genre (display_order);

CREATE TABLE genre_localization (
    genre_code VARCHAR(64)  NOT NULL,
    -- Language の enum コード（ja / en / ko / zh）
    language   VARCHAR(8)   NOT NULL,
    label      VARCHAR(255) NOT NULL,
    CONSTRAINT pk_genre_localization PRIMARY KEY (genre_code, language),
    -- ジャンル削除時はラベルも連動削除する（spot_localization と同じ方針）
    CONSTRAINT fk_genre_localization_genre FOREIGN KEY (genre_code)
        REFERENCES genre (code) ON DELETE CASCADE,
    CONSTRAINT ck_genre_localization_label CHECK (btrim(label) <> '')
);
