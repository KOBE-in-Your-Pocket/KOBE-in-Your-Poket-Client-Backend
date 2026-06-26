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

> Kotlin は最新安定版 2.4.0 ではなく、Spring Boot 4.1.0 が公式に検証・管理する 2.3.21 を採用しています（Spring/JPA コンパイラプラグインの互換性確保のため）。

## ディレクトリ構成

```
src/main/kotlin/com/kobeinyourpocket/backend/
├── KobeBackendApplication.kt   # エントリポイント
└── common/web/                 # 横断的な Web コンポーネント
    └── HealthController.kt      # GET /api/ping
```

## 開発手順

### 1. DB だけ起動してローカルでアプリを実行

```bash
docker compose up -d db          # PostgreSQL を起動
./gradlew bootRun                # アプリをローカル実行 (localhost:8080)
```

### 2. すべて Docker で起動

```bash
docker compose up --build        # DB + アプリをまとめて起動
```

### 3. 動作確認

```bash
curl http://localhost:8080/api/ping
# {"status":"ok","service":"kobe-backend","timestamp":"..."}

curl http://localhost:8080/actuator/health
```

### 4. ビルド / テスト

```bash
./gradlew build                  # テストは H2 (インメモリ) で実行されるため DB 不要
```

## 設定

接続情報などは環境変数で上書きできます。`.env.example` を `.env` にコピーして利用してください。

| 環境変数 | 既定値 | 用途 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/kobe` | DB 接続 URL |
| `SPRING_DATASOURCE_USERNAME` | `kobe` | DB ユーザー |
| `SPRING_DATASOURCE_PASSWORD` | `kobe` | DB パスワード |
| `SPRING_JPA_DDL_AUTO` | `update` | Hibernate のスキーマ自動生成 |
| `SERVER_PORT` | `8080` | アプリのポート |
