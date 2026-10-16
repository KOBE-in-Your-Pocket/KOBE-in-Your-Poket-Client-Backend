-- 利用者によるブロック（#215）。App Store Guideline 1.2 の「悪質なユーザーを利用者が
-- ブロックできる仕組み」。ブロックした側には、ブロックされた側のレビューを表示しない。
--
-- users への外部キーは張らない（reports と同じ方針）。退会時はアプリ側の退会処理で、
-- 本人がした／本人がされたブロックを両方削除する。

CREATE TABLE user_blocks (
    -- ブロックした利用者（Supabase Auth の user id / JWT sub）
    blocker_user_id UUID        NOT NULL,
    -- ブロックされた利用者
    blocked_user_id UUID        NOT NULL,
    -- domain 側で採番したブロック日時をそのまま保存
    created_at      TIMESTAMPTZ NOT NULL,
    -- 同じ相手を 2 回ブロックしても 1 行（ブロック API は冪等）
    CONSTRAINT pk_user_blocks PRIMARY KEY (blocker_user_id, blocked_user_id),
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_user_id <> blocked_user_id)
);

-- 退会時に「本人がされたブロック」を消すため。blocker 側は主キーの先頭列が索引として効く。
CREATE INDEX idx_user_blocks_blocked_user_id ON user_blocks (blocked_user_id);

-- 新規テーブルは RLS を有効にする（#117）。ポリシーは作らないため、Supabase Data API
-- （anon / authenticated ロール）からは読み書きできない。
ALTER TABLE user_blocks ENABLE ROW LEVEL SECURITY;
