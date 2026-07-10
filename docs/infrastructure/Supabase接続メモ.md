# Supabase 接続メモ（Phase 0）

## 取得場所（ダッシュボード）


| 控えるもの            | 画面                                                  | 環境変数名（backend）              |
| ---------------- | --------------------------------------------------- | --------------------------- |
| Project URL      | **Project Settings → API** → Project URL            | `SUPABASE_URL`              |
| anon key         | 同 → Project API keys → `anon` `public`              | `SUPABASE_ANON_KEY`         |
| service_role key | 同 → `service_role` `secret`                         | `SUPABASE_SERVICE_ROLE_KEY` |
| JWT secret       | 同 → JWT Settings → JWT Secret                       | `SUPABASE_JWT_SECRET`       |
| DB 接続（0.4）       | **Project Settings → Database** → Connection string | `SUPABASE_DB_URL` 等         |


## 置き場所


| 環境      | 置き場所                                     | 注意                                        |
| ------- | ---------------------------------------- | ----------------------------------------- |
| ローカル開発  | `KOBE-in-Your-Poket-Client-Backend/.env` | `.gitignore` 済み。`.env.example` はプレースホルダのみ |
| 本番（#78） | EC2 環境変数 / Secrets Manager 等             | GitHub リポジトリ・Client には出さない                |


手順:

```bash
cd KOBE-in-Your-Poket-Client-Backend
cp .env.example .env
# .env の SUPABASE_* をダッシュボードの実値に書き換える
```



## 取り扱いルール

- **Client に** `@supabase/supabase-js` **もキーも入れない**（全部 backend プロキシ）
- `service_role` / JWT secret を Issue・PR・Slack・Notion に貼らない
- チーム共有は「このメモの取得場所」まで。値は各自がダッシュボードから取るか、1on1 で渡す



## DB 接続の注意（0.4）

- EC2 が IPv4 のみ → **Supavisor session mode** `:5432`
- **transaction mode** `:6543` **は使わない**（JPA / Flyway の prepared statement と相性が悪い）



## Phase 0.3 チェック

- [x] `.env.example` に `SUPABASE_*` プレースホルダ追加
- [x] 本メモで取得場所・置き場所をチーム共有可能にした
- [x] `.env` に実値を記入（git にコミットしない）