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

Google / Apple はいずれも id_token グラント中継で対応済み（#89-c / #192）。

- リクエスト: `{"idToken": "...", "accessToken": null, "nonce": null}`（`idToken` 必須。`nonce` はトークン取得時に使った場合のみ）
- 初回ログイン時は GoTrue が Auth ユーザーを自動作成（signup / login の区別なし）。
  プロフィール行は login と同じく冪等補完し、表示名は `user_metadata.full_name` → `user_metadata.name` → email ローカル部 → `"user"`

### Google（`POST /api/v1/auth/google`）

- 事前設定: Supabase ダッシュボード → Authentication → Sign In / Providers → Google を有効化し、
  Client IDs に Google OAuth クライアント ID を登録（カンマ区切りで複数可。トークンの `aud` と照合される）

### Apple（`POST /api/v1/auth/apple`）

- 事前設定: Supabase ダッシュボード → Authentication → Sign In / Providers → Apple を有効化し、
  Services ID / Team ID / Key ID / 秘密鍵（`.p8`）を登録。Client IDs にネイティブアプリの Bundle ID を追記する（#191）
- Apple のネイティブサインインは nonce を使う。Client が nonce を付けた場合は `nonce` が必須（未指定だと GoTrue が 400）
- 表示名（`fullName`）は**初回認証時のみ**返る。2 回目以降は Apple 側から返らないため、
  初回に受け取った値を Client から `name` として渡す必要がある（#193）
- メールアドレスが非公開リレー（`@privaterelay.appleid.com`）になる場合がある

Kakao / LinkedIn / X は後続（`AuthGateway.signInWithIdToken` の provider 引数で拡張する）
