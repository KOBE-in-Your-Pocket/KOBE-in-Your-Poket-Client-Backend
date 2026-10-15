-- Apple の refresh token（#213）。
--
-- Sign in with Apple を提供するアプリは、退会時に Apple の REST API（/auth/revoke）で
-- トークンを失効させる必要がある（App Store 審査の要件）。失効には refresh token が要るため、
-- サインイン時に Client から受け取った authorizationCode を交換して保存しておく。
--
-- 1 ユーザー 1 件。サインインのたびに最新の値へ置き換える。
-- 平文で持つ。この token だけでは Apple の API を呼べず（.p8 で署名した client secret が要る）、
-- Apple はこの token で取得できる利用者データも提供しないため。
-- 退会時はアプリ側で失効させてから削除する。プロフィール行の削除に合わせて消えるよう外部キーも張る。

CREATE TABLE apple_refresh_tokens (
    -- Supabase Auth の user id（users.id）
    user_id       UUID        NOT NULL,
    refresh_token TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_apple_refresh_tokens PRIMARY KEY (user_id),
    CONSTRAINT fk_apple_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
