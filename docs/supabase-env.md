# Supabase 環境変数（Phase 0）

キー本体はリポジトリに入れない。`.env.example` を `.env` にコピーし、ダッシュボードの実値を記入する。

## 取得場所

Supabase ダッシュボード → **Project Settings → API**

| 環境変数 | 画面上の項目 |
| --- | --- |
| `SUPABASE_URL` | Project URL |
| `SUPABASE_ANON_KEY` | `anon` `public` |
| `SUPABASE_SERVICE_ROLE_KEY` | `service_role` `secret` |
| `SUPABASE_JWT_SECRET` | JWT Settings → JWT Secret |

## ルール

- `.env` は gitignore 済み。コミットしない
- Client・Issue・PR に `service_role` / JWT secret を出さない
- チーム向けの詳細（DB session mode 等）は [`infrastructure/Supabase接続メモ.md`](./infrastructure/Supabase接続メモ.md)
