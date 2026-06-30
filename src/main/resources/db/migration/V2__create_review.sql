-- feature ②（tourism / Review）のスキーマ。
-- spot に対してユーザーが投稿する星評価とコメントを保持する。
-- author は PublicUser 仕様確定前のため author_name / author_icon_url の
-- フラットなカラムで表現し、仕様確定後に変更しやすい構造にしている。

CREATE TABLE review (
    id              UUID         NOT NULL,
    spot_id         VARCHAR(128) NOT NULL,
    -- ReviewRating（1〜5）
    rating          SMALLINT     NOT NULL,
    -- ReviewComment（最大 1000 文字）
    comment         TEXT         NOT NULL,
    -- ReviewAuthor.name（最大 100 文字）
    author_name     VARCHAR(100) NOT NULL,
    -- ReviewAuthor.iconUrl（未設定時は空文字。PublicUser 確定後に実 URL へ更新する）
    author_icon_url TEXT         NOT NULL DEFAULT '',
    -- Language コード（ja / en / ko / zh）
    language        VARCHAR(8)   NOT NULL,
    -- domain 側で採番した投稿日時をそのまま保存
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_review PRIMARY KEY (id),
    CONSTRAINT fk_review_spot FOREIGN KEY (spot_id)
        REFERENCES spot (id) ON DELETE CASCADE,
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_review_comment_length CHECK (char_length(comment) <= 1000),
    CONSTRAINT ck_review_author_name_length CHECK (char_length(author_name) <= 100)
);

CREATE INDEX idx_review_spot_id ON review (spot_id);
