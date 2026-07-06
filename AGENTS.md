# AGENTS.md — AI / 人間共通 開発・レビュー基準

このファイルは AI コーディングエージェントおよび PR レビュー（[CodeRabbit](https://github.com/apps/coderabbitai) 一次レビュー・人間レビュー共通）の判断基準です。
詳細な設計思想は [`docs/architecture.md`](docs/architecture.md)、概要は [`README.md`](README.md) を参照してください。

## プロジェクト概要

- 神戸市特化の外国人観光客向けアプリ「KOBE in Your Pocket」のバックエンド REST API
- 観光・マナー啓発・防災（避難）情報を多言語で Expo クライアントへ供給する
- Kotlin 2.3 / Spring Boot 4.1 / JDK 17 / Gradle（Kotlin DSL）/ PostgreSQL・Flyway
- クライアント: [KOBE-in-Your-Poket-Client](https://github.com/KOBE-in-Your-Pocket/KOBE-in-Your-Poket-Client)
- PR は `develop` 向け。コミットは `prefix: summary #issue` 形式

## 開発コマンド（Docker 中心）

ローカルに JDK / Gradle は不要。Docker があれば動く。

- `make dev` — 開発起動（DB + ソースから bootRun）
- `make up` — 本番相当起動（DB + jar、localhost:8080）
- `make test` — テスト（Docker 上の Gradle / H2）
- `make lint` — ktlint チェック
- `make format` — ktlint 自動整形
- `make build` — jar 生成

## アーキテクチャ（最重要）

**Onion Architecture（layer-first + CQRS-lite）** を守る。

- 依存方向: `infrastructure/rest → application → domain ← infrastructure/{persistence,query}`
- ドメイン層はフレームワーク非依存（Spring / JPA を持ち込まない）
- 配置先の判断:
  - HTTP 境界（Controller / DTO） → `infrastructure/rest/{context}/`
  - ユースケース → `application/{context}/{command,query}/`
  - ドメインモデル（不変条件・値オブジェクト・集約） → `domain/{context}/`
  - 書き込み永続化（JPA） → `infrastructure/persistence/{context}/`
  - 読み取り Query（CQRS-lite の read） → `infrastructure/query/{context}/`
- **CQRS-lite**: command は domain 経由で永続化、query は `infrastructure/query` から直接読む（ドメイン層を経由しない）
- コンテキスト: `tourism` / `manner` / `evacuation` / `contentsubmission` / `user`

## ドメインモデル設計方針

- ドメインモデルは **Client の Mock API スキーマを正** として設計する（`docs/architecture.md` §7.0）
- レスポンス DTO は Client の API スキーマと整合させる
- 多言語（ローカライズ）は仕様に従い、フォールバックを考慮する

## コード品質

- ユーザー向け・外部公開のデータは多言語対応を確認
- トランザクション境界・冪等性・ドメイン不変条件の保護を確認
- N+1・遅延ロードなど永続化のパフォーマンス問題に注意
- 不要なコード・デバッグログ・未使用 import を残さない

## データベース / マイグレーション

- Flyway。**適用済みバージョン（`V{n}__*.sql`）は変更禁止** — 新規 `V{n}__` を追加する
- NOT NULL・外部キー・インデックス・多言語テーブルの整合性を確認
- ロールバック不可を前提にレビューする

## テスト

- JUnit5。ビジネスロジック・ユースケースにテストを追加
- 境界値・異常系（未検出・権限・空データ・多言語フォールバック）のエッジケースを考慮
- テスト名・アサーションが仕様意図を表現しているか

## セキュリティ

- API キー・認証情報のハードコード禁止（`.env` / 環境変数経由）
- センシティブデータのログ出力禁止
- 入力検証・エラーレスポンスの適切性を確認

## CI で既に担保されている項目（レビュー不要）

- ktlint (`./gradlew ktlintCheck`)
- Kotlin 型チェック（コンパイル）
- テスト (`./gradlew test`)

これらは指摘せず、設計・ロジック・仕様妥当性・データ整合性に集中すること。
