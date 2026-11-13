-- お気に入り（#235 / PBI #227）。利用者 × 観光スポット。
--
-- spot へは外部キーを張り、スポットの削除に合わせて消す（review と同じ。同じ Tourism コンテキスト内の参照のため）。
-- users への外部キーは張らない（user_blocks・reports と同じ方針）。退会時はアプリ側の退会処理で
-- 本人のお気に入りを削除する（#238）。

CREATE TABLE favorites (
    -- お気に入りした利用者（Supabase Auth の user id / JWT sub）
    user_id    UUID         NOT NULL,
    -- Tourism の Spot.id
    spot_id    VARCHAR(128) NOT NULL,
    -- domain 側で採番した登録日時。一覧を新しい順に並べるのに使う（Client#789）
    created_at TIMESTAMPTZ  NOT NULL,
    -- 同じスポットを 2 回登録しても 1 行（登録 API は冪等）。
    -- 一覧は user_id で引くため、主キーの先頭列がそのまま索引として効く。
    CONSTRAINT pk_favorites PRIMARY KEY (user_id, spot_id),
    CONSTRAINT fk_favorites_spot FOREIGN KEY (spot_id) REFERENCES spot (id) ON DELETE CASCADE
);

-- 新規テーブルは RLS を有効にする（#117）。ポリシーは作らないため、Supabase Data API
-- （anon / authenticated ロール）からは読み書きできない。
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;
