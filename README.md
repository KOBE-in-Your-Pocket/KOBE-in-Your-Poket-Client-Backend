# KOBE-in-Your-Poket-Client-Backend

神戸市特化の外国人観光客向けアプリ **KOBE in Your Pocket** のバックエンド API。
観光・マナー啓発・防災（避難）情報を多言語で提供する REST API を、Expo クライアントへ供給する。

- クライアント: [KOBE-in-Your-Poket-Client](https://github.com/KOBE-in-Your-Pocket/KOBE-in-Your-Poket-Client)
- 仕様書: [Specification](https://github.com/KOBE-in-Your-Pocket/Specification)
- 設計思想: [`docs/architecture.md`](docs/architecture.md)

> ステータス: 初期開発（MVP）。API・スキーマは変更前提。

---

## 提供するもの

| 領域 | 内容 | 状態 |
| --- | --- | --- |
| Tourism（観光） | 観光名所の登録・一覧（多言語）、ジャンル分類 | 実装中 |
| Manner（マナー） | マナー/ルールの啓発コンテンツ | 未着手 |
| Evacuation（避難） | 避難所情報・最寄り検索・オフライン差分同期 | 実装中 |
| User / 認証 | 利用者・運営ロール、ログイン | 未着手 |

クライアントとの境界づけられたコンテキストは Specification `07_bounded-contexts.md` に準拠する。

---

## アーキテクチャ

**Onion Architecture（layer-first + CQRS-lite）** を採用。

```
infrastructure/rest/tourism  →  application/tourism/{command,query}  →  domain/tourism
                                        ↓ read
                                 infrastructure/query/tourism
                                        ↓ write
                                 infrastructure/persistence/tourism
```

- ドメインモデルは **Client の Mock API スキーマ**（`features/{context}/infrastructure/api/mock-*.ts` および `domain/*.ts`）を参照して設計する（[`docs/architecture.md` §7.0](docs/architecture.md#70-ドメインモデル作成方針client-mock-api-を正とする)）
- フロントの Modular Monolith はバックエンドの規模に対し過大として不採用
- レイヤ依存は将来 ArchUnit で機械的に強制する（feature 実装時に導入）

詳細・採用しなかった選択肢は [`docs/architecture.md`](docs/architecture.md) を参照。

---

## 技術スタック

| 項目 | バージョン |
| --- | --- |
| Kotlin | 2.3.21 |
| Spring Boot | 4.1.0 |
| Spring Data JPA | (Spring Boot 管理) |
| Flyway | (Spring Boot 管理) |
| Gradle | 9.6.0 (wrapper) |
| Java (toolchain) | 17 |
| PostgreSQL | 17 |
| ktlint (gradle) | 14.2.0 |

> Kotlin は最新安定版ではなく、Spring Boot 4.1.0 が公式に検証・管理する 2.3.21 を採用（Spring/JPA コンパイラプラグインの互換性確保のため）。

---

## ディレクトリ構成

```
src/main/kotlin/com/kobeinyourpocket/backend/
├── KobeBackendApplication.kt   # エントリポイント
├── domain/                     # コンテキスト別ドメイン（tourism, evacuation, …）
│   └── tourism/{vo,aggregate,repository}/
├── application/                # ユースケース（CQRS: command / query）
│   └── tourism/{command,query}/
└── infrastructure/
    ├── persistence/tourism/
    ├── query/tourism/
    └── rest/
        ├── common/             # 横断 REST（/api/ping 等）
        └── tourism/

src/main/resources/
├── application.yml             # 設定 (環境変数で上書き可)
└── db/migration/               # Flyway マイグレーション (V__*.sql)
```

---

## 開発手順

ローカルに JDK / Gradle は不要。**Docker さえあれば**すべて回る（`make help` で一覧）。

```bash
make dev      # 開発: DB + アプリ(ソースから bootRun) を起動
make up       # 本番相当: DB + アプリ(jar) をバックグラウンド起動
make test     # テスト (Docker 上の Gradle / H2)
make lint     # ktlint チェック
make format   # ktlint 自動整形
make down     # 停止 (make clean で DB データも削除)
```

> アプリは **9090** 番ポートで起動する（Metro/Expo の 8081 系と衝突しないようずらしている）。

### 動作確認

```bash
curl http://localhost:9090/api/ping
# {"status":"ok","service":"kobe-backend","timestamp":"..."}
curl http://localhost:9090/actuator/health
```

---

## API

ベース URL は `/api/v1`。言語は `?lang=`（`ja`/`en`/`ko`/`zh`、無指定は `en` フォールバック。Client `FALLBACK_LANGUAGE` と一致）。

| メソッド | パス | 概要 | 状態 |
| --- | --- | --- | --- |
| GET | `/api/ping` | ヘルスチェック | ✅ |
| GET | `/api/v1/tourism/spots?lang=ja` | 観光名所一覧（解決済み多言語） | ✅ |
| POST | `/api/v1/tourism/spots` | 観光名所（ピン）の登録 | ✅ |

レスポンス形はクライアントのモック（`fetchSpots`）に整合させる。詳細は `docs/architecture.md` の API 契約を参照。

---

## データ出典・ライセンス

避難所（Evacuation）データは公的オープンデータで seed している。二次利用にあたり出典表記が必須のため、
アプリ内表示・ドキュメントの両方でクレジットを保持すること。

| データ | 出典 | ライセンス | データ基準日 |
| --- | --- | --- | --- |
| 避難所（`shelter` / `shelter_localization`） | [神戸市オープンデータポータル「神戸市避難場所」](https://catalog.city.kobe.lg.jp/dataset/evacuation) | クリエイティブ・コモンズ 表示 2.1 日本（[CC BY 2.1 JP](https://data.city.kobe.lg.jp/policy)） | 2025-04-02 |

- 出典表記の要件は神戸市の定める [オープンデータの利用について](https://data.city.kobe.lg.jp/policy) に従う（書式の指定は無いが「神戸市の著作物あるいはデータを使用した旨」の明記が必須）
- 免責: CC ライセンスは著作権上の許諾のみを示すものであり、データの正確性・最新性を神戸市が保証するものではない
- 出典・データ基準日は `shelter_dataset_metadata` テーブル（`V8`/`V9` マイグレーション）で保持し、一覧 API のレスポンス meta として返す想定（#67 で配線）
- 全市の避難所のうち種別・カテゴリの多様性を確保できる代表 11 件のみを初回投入している。全件取込は将来、取込スクリプト化する際に改めて検討する（#66）

---

## DB マイグレーション (Flyway)

スキーマは Flyway が所有する。`src/main/resources/db/migration/` に `V<番号>__<説明>.sql`
を置くと、`make up` / `make dev` の起動時に自動適用される。Hibernate は `ddl-auto=validate`
でエンティティとスキーマの整合のみ検証する（テストは H2 + `create-drop`、Flyway 無効）。

---

## テスト / CI

- `make test` … JUnit5 を H2 上で実行（DB 不要）
- CI（GitHub Actions、PR/push 時）
  - `lint.yml` … `ktlintCheck`
  - `test.yml` … `gradle test`

---

## 設定

接続情報などは環境変数で上書きできる。`.env.example` を `.env` にコピーして利用する。
Supabase キーの取得場所・置き場所は [`docs/supabase-env.md`](./docs/supabase-env.md) を参照（キー本体はリポジトリに入れない）。
CD（ECR + SSM）の説明は [`docs/infrastructure/cd-ecr-ssm.md`](./docs/infrastructure/cd-ecr-ssm.md) を参照。開発用 EC2 の使い方は下記「開発用 EC2 / CD（チーム共有）」を参照。

| 環境変数 | 既定値 | 用途 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/kobe` | DB 接続 URL |
| `SPRING_DATASOURCE_USERNAME` | `kobe` | DB ユーザー |
| `SPRING_DATASOURCE_PASSWORD` | `kobe` | DB パスワード |
| `SPRING_JPA_DDL_AUTO` | `validate` | Hibernate のスキーマ検証（スキーマは Flyway が所有） |
| `SERVER_PORT` | `9090` | アプリのポート（Metro/Expo 8081 系との衝突回避） |
| `SUPABASE_URL` | （未設定） | Supabase Project URL |
| `SUPABASE_ANON_KEY` | （未設定） | anon / public API key |
| `SUPABASE_SERVICE_ROLE_KEY` | （未設定） | service_role（backend 専用） |
| `SUPABASE_JWT_SECRET` | （未設定） | JWT 検証用 secret |

---

## 開発用 EC2 / CD（チーム共有）

更新日: 2026-07-13（関連: #78）

### API エンドポイント（開発）

| 用途 | URL |
| --- | --- |
| Health | `http://18.181.34.28:9090/actuator/health` |
| Spots 例 | `http://18.181.34.28:9090/api/v1/tourism/spots` |

- HTTPS 未設定（HTTP）
- インターネットから届くので、認証前の書き込み API には注意

### 稼働時間（自動）

- **平日 9:00–18:00（JST）** のみ自動起動
- **平日 18:00 以降・土日** は止まっている（課金節約）
- その時間帯に触る／CD を流すときは **手動起動が必要**

### いまの構成（要約）

| 項目 | 内容 |
| --- | --- |
| インスタンス | **`t3.small`（x86_64 / amd64）** |
| インスタンス ID | `i-033fbb6ce4e9f49ae`（AWS 操作する人向け） |
| Elastic IP | `18.181.34.28`（API の向き先。普段はこれだけでよい） |
| CD | `develop` へ push / マージ → **ECR → SSM** でデプロイ（SSH 不要） |
| 秘密情報（開発） | EC2 上の `/opt/kobe-backend/app.env`（Git に入れない） |

**API を叩くだけなら、覚えるのは EIP（上記 URL）と稼働時間で十分です。**  
インスタンス ID は、手動 start/stop や AWS コンソール操作用です。

### 旧構成からの注意

- 旧インスタンス `i-0d0c06a6ced085c4d`（t4g / arm64）は **使わない**（停止済み）
- Docker イメージは **`linux/amd64`**（arm64 イメージは動かない）
- GitHub の `EC2_SSH_KEY` は **CD には不要**（SSM 方式）

### 動作確認

時間内（平日 9–18）か、手動起動後に確認する。

```bash
curl -sS http://18.181.34.28:9090/actuator/health
# 期待: "status":"UP"

curl -sS http://18.181.34.28:9090/api/v1/tourism/spots | head
# JSON が返れば OK
```

- **Client**: API のベース URL を `http://18.181.34.28:9090` にする（ローカル backend を向いていないか）
- **CD 後（Backend 担当）**: GitHub Actions の `deploy` が成功していること、上記 health / spots が通ること

### EC2 の手動起動・停止

前提: ローカルに AWS CLI があり、認証済み（`aws login` または `aws configure`）。  
リージョンは **東京（`ap-northeast-1`）**。

```text
インスタンス ID: i-033fbb6ce4e9f49ae
Elastic IP:     18.181.34.28
```

起動:

```bash
aws ec2 start-instances \
  --instance-ids i-033fbb6ce4e9f49ae \
  --region ap-northeast-1
```

起動確認:

```bash
aws ec2 describe-instances \
  --instance-ids i-033fbb6ce4e9f49ae \
  --region ap-northeast-1 \
  --query 'Reservations[0].Instances[0].[State.Name,PublicIpAddress]' \
  --output text
```

`running` と `18.181.34.28` になれば OK。アプリ起動まで **1〜2 分**かかることがあるので、続けて health を確認する。

停止:

```bash
aws ec2 stop-instances \
  --instance-ids i-033fbb6ce4e9f49ae \
  --region ap-northeast-1
```

停止中も **EBS・Elastic IP の料金はかかる**（計算時間だけ止まる）。

### `.env` / 秘密情報

| 誰 | やること |
| --- | --- |
| **Client 開発** | API 向き先を `http://18.181.34.28:9090` にする（プロジェクトの env 名に合わせる）。**`service_role` / JWT secret は Client に入れない** |
| **Backend ローカル** | 従来どおりローカル `.env`（[`docs/supabase-env.md`](./docs/supabase-env.md) 参照）。EC2 用ではない |
| **EC2 上の設定** | `/opt/kobe-backend/app.env` は運用担当が配置済み。**全員が書き換える必要はない**。キー追加時は担当に依頼 |

- `.env` / `app.env` / `.pem` は **Git にコミットしない**
- Supabase の強い鍵は **backend 経由のみ**

### CD（ざっくり）

1. `develop` にマージ（または push）
2. GitHub Actions が **amd64 イメージを ECR に push**
3. SSM で EC2 が pull → コンテナ再起動
4. health check

注意:

- **EC2 が停止中だとデプロイは失敗し得る** → 先に手動 start
- 平日 18 時直前のマージは、停止スケジュールと重なることがある

詳細: [`docs/infrastructure/cd-ecr-ssm.md`](./docs/infrastructure/cd-ecr-ssm.md) / [`.github/workflows/deploy.yml`](./.github/workflows/deploy.yml)

### DB マイグレーション（開発 EC2）

- `develop` に載った新しい `V*.sql` は、**デプロイ後のアプリ起動時に Flyway が自動適用**
- Supabase コンソールで手動マイグレーションする必要はない
- **適用済みの `V{n}__*.sql` は変更禁止**（新規ファイルを追加）

### 困ったとき

| 症状 | 確認 |
| --- | --- |
| curl がタイムアウト | 稼働時間外ではないか → 手動 start |
| health が UP にならない | start 直後なら少し待つ。それでもダメなら担当へ |
| CD 失敗 | Actions ログ。EC2 が `running` / SSM Online か |
| Client だけ繋がらない | API URL が EIP になっているか |

AWS / インフラ操作に自信がない場合は、無理に触らず担当に依頼してください。

---

## ブランチ / コミット規約

GitFlow ベース（Specification `04_dev-rules.md` に準拠）。

- `main`（リリース）/ `develop`（統合）/ `feat|fix|chore/...`（作業）
- PR は `develop` 向け。`main` への直接 push は禁止
- コミット: `<prefix>: <概要>`（`feat` / `fix` / `docs` / `chore` / `refactor` / `test` / `style`）

---

## 実装ロードマップ

1. **観光ピン登録**（Tourism） … `Spot` の登録・一覧
2. **レビュー** … 観光名所への評価・コメント投稿（Client #129）
3. **ログイン** … 認証・ロール（レビュー投稿の本人性担保）
