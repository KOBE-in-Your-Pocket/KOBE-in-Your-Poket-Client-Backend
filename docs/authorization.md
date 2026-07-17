# 書き込み認可と運営ロール運用（#90）

書き込み系（POST / PUT / PATCH / DELETE）は **deny-by-default**。
認可ルールに明示的にマッチしないリクエストは運営（OPERATOR）ロールが必要になる。
実装は `infrastructure/security/SecurityConfig`。

## 認可ポリシー（エンドポイント別）

| エンドポイント | 認可 |
| --- | --- |
| `POST /api/v1/auth/signup` `login` `google` `refresh` | 公開（認証の入り口） |
| `POST /api/v1/auth/logout` | 認証必須 |
| `POST /api/v1/tourism/spots/{spotId}/reviews`・`PUT .../reviews/{reviewId}` | 認証済みユーザー（一般ロール可） |
| `DELETE /api/v1/auth/users/{userId}` | ADMIN のみ（`@PreAuthorize` / #137） |
| 上記以外の書き込み全て（`POST /api/v1/tourism/spots` 等、今後追加分も含む） | **運営（OPERATOR）ロール必須** |
| GET 系 | 公開 |

## ロール階層

`ADMIN > OPERATOR > GENERAL`（`SecurityConfig.roleHierarchy`）。

- ADMIN は運営系書き込み（スポット登録等）も実行できる
- OPERATOR は一般ユーザー操作（レビュー投稿等）も実行できる
- ADMIN 専用操作（ユーザー削除）は OPERATOR では 403

ロールの正は Supabase JWT の `app_metadata.role` クレーム（#15 / [`supabase-jwt.md`](./supabase-jwt.md)）。
未設定・未対応の値は一般（GENERAL）として扱う。

## 401 / 403 レスポンス

Resource Server 既定のレスポンスは `ApiAuthenticationEntryPoint` / `ApiAccessDeniedHandler` で
統一エラー形式（§3.3 / #24）に差し替えている。

```json
{ "status": 401, "error": "Unauthorized", "message": "Authentication is required", "violations": [] }
{ "status": 403, "error": "Forbidden", "message": "Access is denied", "violations": [] }
```

- **401**: Bearer なし / 署名・期限が不正
- **403**: 認証済みだがロール不足

## 運営ロールの付与手順

対象ユーザーの `app_metadata` に `"role": "operator"` を設定する。
`app_metadata` はユーザー本人が変更できない（`user_metadata` と違い Admin API / service_role のみ）ため、認可の根拠にできる。

### 事前準備

| 必要なもの | 取得場所 |
| --- | --- |
| ユーザー UUID | ダッシュボード Authentication > Users（メールアドレスで検索） |
| `SUPABASE_URL` | ダッシュボード Settings > API |
| `SUPABASE_SERVICE_ROLE_KEY` | ダッシュボード Settings > API（**secret**。コミット・クライアント配布禁止） |

### 方法 1: スクリプト（推奨）

```bash
SUPABASE_URL=https://xxxx.supabase.co \
SUPABASE_SERVICE_ROLE_KEY=eyJ... \
./scripts/set-user-role.sh <user-uuid> operator
```

ロールは `general` / `operator` / `admin` を指定できる（降格は `general` を設定）。

### 方法 2: Admin API を直接叩く

```bash
# レスポンスには email 等の PII が含まれるため破棄し、HTTP ステータスのみ表示する
curl -fsS -o /dev/null -w '%{http_code}\n' -X PUT "$SUPABASE_URL/auth/v1/admin/users/<user-uuid>" \
  -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
  -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
  -H "Content-Type: application/json" \
  -d '{"app_metadata":{"role":"operator"}}'
```

`app_metadata` はトップレベルキー単位でマージされるため、`role` 以外のキーは保持される。

### 方法 3: ダッシュボードの SQL Editor

```sql
update auth.users
set raw_app_meta_data = raw_app_meta_data || '{"role": "operator"}'::jsonb
where id = '<user-uuid>';
```

### 反映タイミング

`app_metadata` は **JWT 発行時に焼き込まれる**。付与後、対象ユーザーが再ログインまたは
`POST /api/v1/auth/refresh` で新しい access token を取得するまで反映されない。

## 動作確認

```bash
# 未認証の運営書き込み → 401
curl -i -X POST http://localhost:9090/api/v1/tourism/spots \
  -H "Content-Type: application/json" -d '{}'

# 一般ユーザーの運営書き込み → 403
curl -i -X POST http://localhost:9090/api/v1/tourism/spots \
  -H "Authorization: Bearer <一般ユーザーの access token>" \
  -H "Content-Type: application/json" -d '{}'

# 運営ユーザー → 400（バリデーション到達）または 201
```
