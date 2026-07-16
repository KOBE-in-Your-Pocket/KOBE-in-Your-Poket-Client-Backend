# Supabase JWT 検証（#89）

backend は Spring Security **OAuth2 Resource Server** として、Supabase Auth が発行した JWT を検証する。
自前のトークン発行・パスワード管理は行わない。

## 検証方式（JWKS / Signing Keys）

現行の Supabase プロジェクトは **JWT Signing Keys（例: ES256）** を使う。
access token の header に `alg: ES256` と `kid` が付くため、共有秘密鍵（HS256）では検証できない。

| 項目 | 内容 |
| --- | --- |
| 方式 | **JWKS**（公開鍵） |
| 取得先 | `{SUPABASE_URL}/auth/v1/.well-known/jwks.json` |
| アルゴリズム | **ES256**（Signing Keys）。Spring の JWKS decoder 既定は RS256 のみなので `discoverJwsAlgorithms()` が必要 |
| 環境変数 | `SUPABASE_URL`（必須・ローカル/本番） |
| 実装 | `NimbusJwtDecoder.withJwkSetUri`（`infrastructure/security/SecurityConfig`） |

`SUPABASE_URL` が空のときだけ、レガシー互換で **HS256 + `SUPABASE_JWT_SECRET`** にフォールバックする（主にユニットテスト）。

### 以前の JWT Secret（HS256）について

ダッシュボードの JWT Secret はレガシー方式。Signing Keys 移行後のトークンは Secret では検証できない。
Bearer 付きリクエストが 401 になる場合、まず JWT header の `alg` が `ES256` か確認する。

## ロールクレーム

| JWT | Spring authority | domain |
| --- | --- | --- |
| `app_metadata.role` = `admin` | `ROLE_ADMIN` | [Role.ADMIN] |
| `app_metadata.role` = `operator` | `ROLE_OPERATOR` | [Role.OPERATOR] |
| 未設定 / その他 | `ROLE_GENERAL` | [Role.GENERAL] |

ロール階層は `ADMIN > OPERATOR > GENERAL`（`SecurityConfig.roleHierarchy`）。

## 認可（#90 適用済み）

書き込み系（POST / PUT / PATCH / DELETE）は **deny-by-default**。

- GET 系は公開
- `POST /api/v1/auth/signup|login|google|refresh` は公開（認証の入り口）
- `POST /api/v1/auth/logout` とレビュー投稿・更新は認証必須（一般ロール可）
- 上記以外の書き込みは運営（OPERATOR）ロール必須

エンドポイント別の一覧・運営ロール付与の運用手順は [`authorization.md`](./authorization.md)。
401/403 は統一エラー形式（`{ status, error, message, violations }`）で返る。
不正な Bearer は従来どおり 401。

## ローカル起動

`.env` に実値の `SUPABASE_URL`（と Auth プロキシ用の `SUPABASE_ANON_KEY`）が必要。
JWKS 検証だけなら `SUPABASE_JWT_SECRET` は必須ではないが、既存ドキュメント・EC2 設定との互換のため残してよい。
取得場所は [`supabase-env.md`](./supabase-env.md)。
