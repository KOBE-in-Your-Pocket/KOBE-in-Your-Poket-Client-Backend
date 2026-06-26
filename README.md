# KOBE-in-Your-Poket-Client-Backend

KOBE in Your Pocket のバックエンド側リポジトリです。

クライアント: [KOBE-in-Your-Poket-Client](https://github.com/KOBE-in-Your-Pocket/KOBE-in-Your-Poket-Client)
仕様書: [Specification](https://github.com/KOBE-in-Your-Pocket/Specification)

## 技術スタック

| 項目 | バージョン |
| --- | --- |
| Kotlin | 2.3.21 |
| Spring Boot | 4.1.0 |
| Spring Data JPA | (Spring Boot 管理) |
| Gradle | 9.6.0 (wrapper) |
| Java (toolchain) | 17 |
| PostgreSQL | 17 |
| Flyway | (Spring Boot 管理) |
| ktlint (gradle) | 14.2.0 |

> Kotlin は最新安定版 2.4.0 ではなく、Spring Boot 4.1.0 が公式に検証・管理する 2.3.21 を採用しています（Spring/JPA コンパイラプラグインの互換性確保のため）。

## ディレクトリ構成

```
src/main/kotlin/com/kobeinyourpocket/backend/
├── KobeBackendApplication.kt   # エントリポイント
├── common/web/                 # 横断的な Web コンポーネント (GET /api/ping 等)
└── tourism/                    # コアドメイン (フル Onion)
    ├── domain/                 #   model / repository (純粋 Kotlin)
    ├── application/            #   ユースケース
    └── infrastructure/         #   persistence (JPA) / web (REST)
# evacuation / manner / user / contentsubmission / qronboarding は雛形のみ
```

> 設計の詳細は [`docs/architecture.md`](docs/architecture.md) を参照。

## 開発手順

ローカルに JDK / Gradle は不要。**Docker さえあれば**すべて回る (`make help` で一覧)。

```bash
make dev      # 開発: DB + アプリ(ソースから bootRun) を起動
make up       # 本番相当: DB + アプリ(jar) をバックグラウンド起動
make test     # テスト (Docker 上の Gradle / H2)
make lint     # ktlint チェック
make format   # ktlint 自動整形
make down     # 停止 (make clean で DB データも削除)
```

> アプリは **9090** 番ポートで起動する (Metro/Expo の 8081 系と衝突しないようずらしている)。

### 動作確認

```bash
curl http://localhost:9090/api/ping
# {"status":"ok","service":"kobe-backend","timestamp":"..."}
curl http://localhost:9090/actuator/health
```

### DB マイグレーション (Flyway)

スキーマは Flyway が管理する。`src/main/resources/db/migration/` に `V<番号>__<説明>.sql`
を置くと、`make up` / `make dev` の起動時に自動適用される。Hibernate は `ddl-auto=validate`
でエンティティとスキーマの整合のみ検証する (テストは H2 + `create-drop`、Flyway 無効)。

## 設定

接続情報などは環境変数で上書きできます。`.env.example` を `.env` にコピーして利用してください。

| 環境変数 | 既定値 | 用途 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/kobe` | DB 接続 URL |
| `SPRING_DATASOURCE_USERNAME` | `kobe` | DB ユーザー |
| `SPRING_DATASOURCE_PASSWORD` | `kobe` | DB パスワード |
| `SPRING_JPA_DDL_AUTO` | `validate` | Hibernate のスキーマ検証 (スキーマは Flyway が所有) |
| `SERVER_PORT` | `9090` | アプリのポート (Metro/Expo 8081 系との衝突回避) |
