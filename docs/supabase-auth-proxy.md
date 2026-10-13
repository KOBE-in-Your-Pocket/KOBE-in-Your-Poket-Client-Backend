# Supabase Auth プロキシ（#89-b）

Client は Supabase と直接通信しない。backend が GoTrue を中継する。

```text
Client → POST /api/v1/auth/* → backend → {SUPABASE_URL}/auth/v1/*
```

## エンドポイント

| method | path | GoTrue | 備考 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/signup` | `/auth/v1/signup` | 成功時に `users` プロフィール行を作成 |
| POST | `/api/v1/auth/login` | `/auth/v1/token?grant_type=password` | |
| POST | `/api/v1/auth/google` | `/auth/v1/token?grant_type=id_token` | Google ID トークンで signup / login 兼用（#89-c） |
| POST | `/api/v1/auth/apple` | `/auth/v1/token?grant_type=id_token` | Apple ID トークンで signup / login 兼用（#192） |
| POST | `/api/v1/auth/facebook` | `/auth/v1/token?grant_type=id_token` | Facebook ID トークンで signup / login 兼用（#196） |
| POST | `/api/v1/auth/refresh` | `/auth/v1/token?grant_type=refresh_token` | |
| POST | `/api/v1/auth/logout` | `/auth/v1/logout` | `Authorization: Bearer <access_token>` |

## 環境変数

| 変数 | 用途 |
| --- | --- |
| `SUPABASE_URL` | Project URL |
| `SUPABASE_ANON_KEY` | GoTrue 呼び出し用 apikey（anon） |
| `SUPABASE_JWT_SECRET` | Resource Server の JWT 検証（#89-a） |

`service_role` は本プロキシの Email フローでは使わない（Admin 操作用に別途保持）。

## レスポンス（セッション）

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "tokenType": "bearer",
  "user": { "id": "...", "name": "...", "iconUrl": null }
}
```

メール確認が有効な場合、signup 直後は `accessToken` が null のことがある（Supabase 設定依存）。
プロフィール行は Auth ユーザー id が取れれば作成する。
Auth 作成後にプロフィール保存だけ失敗した場合は signup 内で有限リトライし、それでも欠けるときは **login** が同じ Auth id で冪等に補完する（再 signup 不要）。
Auth ユーザー削除による補償は `service_role` Admin が必要なため本フローでは行わない。

## SSO

Google / Apple / Facebook はいずれも id_token グラント中継で対応済み（#89-c / #192 / #196）。

- リクエスト: `{"idToken": "...", "accessToken": null, "nonce": null, "name": null}`
  （`idToken` 必須。`nonce` はトークン取得時に使った場合のみ。`name` は任意で最大 100 文字、超過は 400）
- 初回ログイン時は GoTrue が Auth ユーザーを自動作成（signup / login の区別なし）。
  プロフィール行は login と同じく冪等補完し、表示名はリクエストの `name` → `user_metadata.full_name` →
  `user_metadata.name` → email ローカル部 → `"user"`
- `name` はプロフィール行の**新規作成時のみ**使う。既存行があれば無視し、表示名を上書きしない
  （表示名の変更は `PATCH /api/v1/users/me`。#179）

### Google（`POST /api/v1/auth/google`）

- 事前設定: Supabase ダッシュボード → Authentication → Sign In / Providers → Google を有効化し、
  Client IDs に Google OAuth クライアント ID を登録（カンマ区切りで複数可。トークンの `aud` と照合される）

### Apple（`POST /api/v1/auth/apple`）

- 事前設定（#191）: Supabase ダッシュボード → Authentication → Sign In / Providers → Apple

  | 項目 | 設定値 | 備考 |
  | --- | --- | --- |
  | Enable Sign in with Apple | オン | ネイティブの ID トークン方式もこのスイッチで有効になる |
  | Client IDs | `com.kobeinyourpocket.client` | id_token の `aud` と照合される。Client の `ios.bundleIdentifier` と一致させる |
  | Secret Key (for OAuth) | 空欄 | Web の OAuth 方式専用。ネイティブ方式のみのため未設定 |
  | Allow users without an email | オフ | Apple はメール非公開時もリレーアドレスを返す |

  - Web の OAuth 方式を追加する場合は次のとおり設定する
    - Client IDs: **Services ID を先頭**に置き、後ろにネイティブの Bundle ID を続ける
      （例: `<Services ID>,com.kobeinyourpocket.client`。OAuth には先頭の値が使われる）
    - Secret Key: Team ID / Key ID / 秘密鍵（`.p8`）から生成した client secret（JWT）を設定する。
      `.p8` 自体は登録しない。JWT は最長 6 か月で失効するため、6 か月ごとに再生成して更新する
    - `.p8`・Key ID・生成した JWT はリポジトリや `.env.example` に入れない
  - Expo Go で取得した id_token は `aud` が Expo Go の Bundle ID（`host.exp.Exponent`）になるため、
    Expo Go で動作確認する場合は Client IDs にカンマ区切りで追記する（現在は未登録）
- Apple のネイティブサインインは nonce を使う。Client が nonce を付けた場合は `nonce` が必須（未指定だと GoTrue が 400）
  - Apple へは raw nonce の SHA-256 を渡し、id_token の `nonce` クレームにはそのハッシュ値が入る。
    backend（GoTrue）へはハッシュ前の **raw nonce** を渡す（GoTrue 側でハッシュして照合する）
- 表示名（`fullName`）は**初回認証時のみ**返り、ID トークンにも含まれない。
  Client は初回に受け取った値を `name` として渡す（#193）。2 回目以降は省略してよい
  - backend 呼び出しが失敗すると、再試行時には Apple が `fullName` を返さない。
    Client は backend の成功まで `fullName` を保持して再送する
- メールアドレスが非公開リレー（`@privaterelay.appleid.com`）になる場合がある。
  `name` が無いとローカル部（ランダム文字列）が表示名になるため、ユーザーは `PATCH /api/v1/users/me` で変更する
- 動作確認: 実機の Apple サインインで得た `identityToken` と raw nonce を使う。
  トークンがシェル履歴に残らないよう、コマンドに直接書かず `read -rs` で変数に入れる

  ```bash
  read -rsp 'identityToken: ' ID_TOKEN; echo
  read -rsp 'raw nonce: ' RAW_NONCE; echo

  # GoTrue へ直接（backend を介さない切り分け用）
  curl -s -X POST "$SUPABASE_URL/auth/v1/token?grant_type=id_token" \
    -H "apikey: $SUPABASE_ANON_KEY" -H "Content-Type: application/json" \
    -d "{\"provider\":\"apple\",\"id_token\":\"$ID_TOKEN\",\"nonce\":\"$RAW_NONCE\"}"

  # backend 経由
  curl -s -X POST http://localhost:8080/api/v1/auth/apple \
    -H "Content-Type: application/json" \
    -d "{\"idToken\":\"$ID_TOKEN\",\"nonce\":\"$RAW_NONCE\"}"

  unset ID_TOKEN RAW_NONCE
  ```

  レスポンスの access / refresh token も共有・記録しない。

  `aud` 不一致なら Client IDs、nonce 不一致ならハッシュ前後の取り違えを疑う

### Facebook（`POST /api/v1/auth/facebook`）

- 事前設定: Supabase の Facebook プロバイダ設定は #195 で行う（Meta アプリの作成は Client #553）
- ネイティブで OIDC の id_token を得るには、iOS は Limited Login、Android は `openid` スコープが必要
- Google / Apple と異なり、GoTrue は `id_token` に加えて `accessToken`（Facebook のアクセストークン）も
  必須とする想定（#195 で確認する）。backend では必須チェックをせず、未指定で GoTrue が拒否した場合は
  その 400 を統一エラー形式で返す
- `email` パーミッションの拒否や電話番号だけのアカウントでは、メールが空になる場合がある。
  表示名は `name` → `user_metadata` → email ローカル部 → `"user"` の順でフォールバックするため失敗はしない

Kakao / LinkedIn / X は後続（`AuthGateway.signInWithIdToken` の provider 引数で拡張する）
