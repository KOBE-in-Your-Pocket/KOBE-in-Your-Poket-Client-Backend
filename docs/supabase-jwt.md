# Supabase JWT 検証（#89）

backend は Spring Security **OAuth2 Resource Server** として、Supabase Auth が発行した JWT を検証する。
自前のトークン発行・パスワード管理は行わない。

## 検証方式（確定: HMAC / JWT Secret）

| 項目 | 内容 |
| --- | --- |
| 方式 | **HS256**（対称鍵） |
| 鍵 | Project Settings → API → **JWT Settings → JWT Secret** |
| 環境変数 | `SUPABASE_JWT_SECRET` → `supabase.jwt.secret` |
| 実装 | `NimbusJwtDecoder.withSecretKey`（`infrastructure/security/SecurityConfig`） |

### JWKS について

JWKS（JSON Web Key Set）は公開鍵一覧を URL から取得して検証する方式。
本プロジェクトはダッシュボードの JWT Secret を使うため **現状は JWKS を使わない**。
将来切替える場合は `JwtDecoder` だけ差し替え、認可ルールはそのままにできる。

## ロールクレーム

| JWT | Spring authority | domain |
| --- | --- | --- |
| `app_metadata.role` = `operator` | `ROLE_OPERATOR` | [Role.OPERATOR] |
| 未設定 / その他 | `ROLE_GENERAL` | [Role.GENERAL] |

`hasRole("OPERATOR")` で運営限定にできる（#90 で `POST /tourism/spots` 等に適用予定）。

## 認可（#89-a 時点）

閲覧・書き込みとも **permitAll**（認証なしで従来どおり利用可）。
Bearer トークンが付いていれば署名検証し、Authentication にロールを載せる。
不正な Bearer は 401。

書き込みの運営限定は **#90**。

## ローカル起動

`.env` に実値の `SUPABASE_JWT_SECRET` が必要（空だと起動時に失敗する）。
取得場所は [`supabase-env.md`](./supabase-env.md)。
