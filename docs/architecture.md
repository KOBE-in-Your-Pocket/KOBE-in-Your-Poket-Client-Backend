# バックエンド アーキテクチャ

> **ステータス：初版（思想確定）**
> フロントの仕様書 `03_architecture.md` / `07_bounded-contexts.md`、および Client のモック API（`fetchSpots`）を前提に、バックエンドのアーキテクチャ思想を確定する。
> 命名規約・テスト戦略の細則は実装フェーズで別途詰める。

---

## 0. このドキュメントの位置づけ

「**なぜこの構造を選んだか**」を残す。読めば次が分かる状態にする：

- アーキテクチャ選定の理由と、採用しなかった選択肢
- 守るべき依存ルール
- どこに何を置くかの判断基準
- フロントの API 契約との対応関係

---

## 1. 設計思想

### 1.1 採用するアーキテクチャ

**Onion Architecture（package-by-layer の単一モジュール）**

- 同心円状にレイヤを重ね、依存方向を常に内側（ドメイン）へ向ける
- パッケージは **レイヤ（domain / application / infrastructure）を上に置き**、その下に bounded context を切る
- コンテキストを跨ぐユースケース・Spring DI の配線がしやすい構成

```
        ┌─────────────────────────────────────┐
        │ infrastructure (web / persistence)  │  ← 外側：FW・DB・HTTP
        │   ┌─────────────────────────────┐   │
        │   │ application (use cases)      │   │
        │   │   ┌─────────────────────┐    │   │
        │   │   │ domain (model+port) │    │   │  ← 内側：純粋 Kotlin
        │   │   └─────────────────────┘    │   │
        │   └─────────────────────────────┘   │
        └─────────────────────────────────────┘
                 依存は常に内向き ▶
```

### 1.2 なぜ Onion か（Modular Monolith を採らない理由）

フロントは Modular Monolith × Clean Architecture を採用しているが、**バックエンドにそのまま鏡像移植はしない**。

| 観点 | 判断 |
|---|---|
| 規模 | backend の責務は実質 CRUD + geo + i18n 解決。コンテキスト間のハード隔離（Spring Modulith・イベント駆動）はコストが価値を上回る |
| チーム | 5〜10 名・同一メンバーがフロント/バックを跨ぐ。Onion の「ドメイン中心＋依存性逆転」だけで十分な統制が効く |
| 拡張性 | レイヤ単位で横断的な application サービスを追加しやすい。コンテキスト追加は各レイヤ配下にサブパッケージを増やす |

> Onion / Hexagonal / Clean は同系統（ドメイン中心・依存性逆転）。重かったのは Modular Monolith レイヤであり、そこだけ外した。

### 1.3 採用しなかった選択肢と棄却理由

| 案 | 棄却理由 |
|---|---|
| package-by-feature（`{context}/domain/...`） | コンテキスト跨ぎの application 配線・Spring DI が煩雑。ドメイン型の横断参照もレイヤ単位の方が見通しが良い |
| Modular Monolith + Spring Modulith | 本 backend の規模に対して境界強制のオーバーヘッドが過大 |
| レイヤドアーキテクチャ（Controller→Service→Repository 直線） | ドメインが DB スキーマに引きずられ、依存性逆転が効かない |
| マイクロサービス | 1 デプロイ単位で十分。コンテキストは後から切り出せる |
| `shared/` 横断バケツ | backend には UI primitives 等の横断対象が無い。何でも箱化を招く（§3.3） |

---

## 2. レイヤと依存ルール

| 層 | 責務 | 依存してよい先 |
|---|---|---|
| **domain** | ドメインモデル・不変条件・Repository interface（port）。純粋 Kotlin | 何にも依存しない（Spring も JPA も知らない） |
| **application** | ユースケース（ドメイン要素を組み合わせた手続き） | domain のみ |
| **infrastructure/persistence** | JPA エンティティ・Repository 実装（outbound adapter） | domain のみ |
| **infrastructure/web** | REST Controller・DTO（inbound adapter） | application, domain |

### 鉄則

- **domain は外側を一切 import しない**（依存性逆転：infrastructure が domain の port を実装する）
- **web は persistence を直接呼ばない**。必ず application を経由
- application が単純転送だけなら省略可（軽量モード。支援サブドメイン向け）
- この依存方向は **ArchUnit** で機械的に強制する（§6）

---

## 3. ディレクトリ構造

### 3.1 パッケージ構成（layer-first）

```
src/main/kotlin/com/kobeinyourpocket/backend/
├── KobeBackendApplication.kt
├── domain/                       # 純粋 Kotlin（FW 非依存）
│   ├── tourism/
│   │   ├── vo/                   # Value Object（Coordinates, SpotId, Genre ...）
│   │   ├── aggregate/            # 集約ルート（Spot 等。vo をコンポジションで保持）
│   │   └── repository/           # SpotRepository (port)
│   ├── evacuation/
│   ├── manner/
│   ├── user/
│   └── contentsubmission/
├── application/                    # ユースケース（domain のみ依存）
│   ├── tourism/                  # SpotService（registerSpot / listSpots）
│   ├── evacuation/
│   └── ...                       # コンテキスト跨ぎの Service は application/ 直下も可
└── infrastructure/
    ├── persistence/              # JPA エンティティ・Repository 実装
    │   ├── tourism/
    │   └── ...
    └── web/                      # REST Controller・DTO
        ├── common/               # GET /api/ping 等の横断 Web 部品
        └── tourism/
```

`evacuation/` `manner/` `user/` `contentsubmission/` も同形のサブパッケージを各レイヤ配下に置く。

### 3.2 判断基準

| 迷ったら | 置き場所 |
|---|---|
| 1 つのコンテキストに閉じる型・port | `domain/{context}/` |
| Value Object（値オブジェクト） | `domain/{context}/vo/` |
| 集約ルート（Entity） | `domain/{context}/aggregate/` |
| ユースケース（単一コンテキスト） | `application/{context}/` |
| 複数コンテキストを組み合わせるユースケース | `application/` 直下（例: `application/sync/`） |
| JPA・DB adapter | `infrastructure/persistence/{context}/` |
| REST・DTO | `infrastructure/web/{context}/` |
| アプリ全体で横断する Web 部品 | `infrastructure/web/common/` |

### 3.3 `shared/` を作らない

フロントの `shared/` は UI primitives・i18next ラッパ等「UI 層の横断物」の置き場で、backend に等価物が無い。横断的に必要なもの（例：グローバル例外ハンドラ `@RestControllerAdvice`）が**実際に複数 feature で必要になった時点で**、`infrastructure/web/common` のように**用途を絞った名前**で切り出す。最初から横断バケツは作らない（YAGNI）。

---

## 4. コンテキスト対応と適用モード

フロント `07_bounded-contexts.md` の区分を踏襲。

| コンテキスト | 区分 | backend の構成 |
|---|---|---|
| Tourism | コア | **フル Onion** |
| Evacuation | コア | **フル Onion**（+ 差分同期・geo） |
| Manner | コア | **フル Onion**（スポット連動マナーは spotId 参照のみ） |
| User | 支援 | 軽量（application 省略可） |
| ContentSubmission | 支援 | 軽量（書き込み・公開状態） |
| Localization | 汎用 | 各 feature 内で言語解決（独立層にしない） |
| GeoLocation | 汎用 | 各 feature の persistence + PostGIS（§7.2） |

---

## 5. 技術スタック

### 5.1 確定済み（環境構築済み）

| 項目 | バージョン | 備考 |
|---|---|---|
| Kotlin | 2.3.21 | Spring Boot 4.1 が公式検証する版 |
| Spring Boot | **4.1.0（GA：2026-06-10）** | Kotlin 2.3 対応。Jackson 3（`tools.jackson`）採用 |
| Java toolchain | 17 | Spring Boot 4 のベースライン |
| Gradle | 9.6.0（wrapper） | Kotlin DSL |
| データアクセス | Spring Data JPA / Hibernate | |
| DB | PostgreSQL 17 | |
| ビルド/配信 | マルチステージ Dockerfile + compose | |

### 5.2 追加予定（安定版を確認済み・実装フェーズで導入）

| 用途 | 依存 | バージョン | 導入タイミング |
|---|---|---|---|
| スキーマ管理 | `org.flywaydb:flyway-core` (+ `flyway-database-postgresql`) | 12.4.0（Spring Boot BOM 管理＝明示不要） | feature ① の初回マイグレーション時。`ddl-auto` を `validate` へ |
| 層依存の強制 | `com.tngtech.archunit:archunit-junit5` | **1.4.2**（BOM 管理外＝明示） | tourism パッケージ作成時 |
| Kotlin テスト | `io.mockk:mockk` | **1.14.11**（BOM 管理外＝明示） | application 層のユニットテスト時 |
| 統合テスト | `org.testcontainers:*`（postgis イメージ） | Spring Boot BOM 管理 | geo を含むテストが必要になった時 |
| 空間データ | `org.hibernate.orm:hibernate-spatial` | 7.4.1.Final（BOM 管理） | 最寄り検索（§7.2）を実装する時 |

> 方針：新規技術導入は必ず最新**安定版**を確認してから入れる。BOM 管理対象はバージョンを明示せず Spring Boot に追従する。

---

## 6. 境界・依存の自動強制

- **ArchUnit (JUnit5)** で以下を CI 必須化する：
  1. `..domain..` は `..application..` / `..infrastructure..` / Spring / JPA に依存しない
  2. `..infrastructure.web..` は `..infrastructure.persistence..` を直接参照しない（application 経由）
  3. レイヤの依存方向（infrastructure → application → domain）を守る
- 段階導入：プロトタイプ期は `warn`、本実装以降は失敗扱い（フロントの boundaries 運用と同調）

---

## 7. データモデル方針

### 7.0 ドメインモデル作成方針（Client Mock API を正とする）

**backend のドメインモデルは、Client 側 Mock API のスキーマを参照して設計する。** これにより REST レスポンスを Client が `mock` から実 fetch へ差し替えたとき、application / UI 層を無改修に保てる。

#### 参照先（Client リポジトリ）

| 優先度 | パス | 内容 |
|---|---|---|
| 1 | `src/features/{context}/infrastructure/api/mock-*.ts` | Mock fetcher の入出力・レスポンス形（API 契約の実体） |
| 2 | `src/features/{context}/domain/*.ts` | Client 側ドメイン型（フィールド名・optional・ネスト構造） |
| 3 | 本ドキュメント §8 | Mock に無い書き込み API 等の backend 独自契約 |

Client リポジトリ: [KOBE-in-Your-Poket-Client](https://github.com/KOBE-in-Your-Pocket/KOBE-in-Your-Poket-Client)

#### 手順

1. 対象コンテキストの `mock-*.ts` と `domain/*.ts` を読み、**API が返す解決済みオブジェクト**の形を把握する
2. `domain/{context}/vo/` に Value Object、`domain/{context}/aggregate/` に集約ルートを定義する（Client Mock API と整合）
3. 永続化（DB テーブル）は API 形と一致させる必要はない（§7.1 の i18n 分割など）。**domain ↔ persistence の変換は infrastructure/persistence に閉じる**
4. REST レスポンス DTO（`infrastructure/web`）は Mock の返却形に合わせ、domain から組み立てる

#### 例: Tourism / Spot

| Client（`domain/spot.ts` + `mock-spots.ts`） | backend |
|---|---|
| `Spot { id, name, genre, description, coordinates, businessHours, category, media, rating? }` | 一覧 API の返却単位。`rating` はレビュー未実装時 `null` |
| `SpotGenre` リテラル列挙 | `domain/tourism/vo` の enum 等で同値を定義 |
| `MOCK_SPOT_BASES` + `MOCK_SPOT_LOCALIZATIONS` の分割 | DB は `spot` + `spot_localization` に分割（§7.1）。API は `lang` 解決後に Client の `Spot` 形で返す |
| `fetchSpots(language): Promise<Spot[]>` | `GET /api/v1/tourism/spots?lang=` の 200 レスポンス |

Mock が未整備のコンテキスト（evacuation / manner 等）は、Client に `domain` 型が追加された時点で同手順を適用する。Client 側に型も Mock も無い場合のみ、Specification / §8 の契約から backend が先行定義する。

### 7.1 多言語（i18n）

モック（`mock-spots` + `mock-spot-localizations`）の「言語非依存ベース + 言語別ローカライズ」分割をテーブルにも踏襲する。`businessHours` はモックで言語側（"24時間"/"Open 24 hours"）なのでローカライズ側に置く。

```
spot(id, genre, latitude, longitude, image_url, rating_value NULL, created_at, updated_at)
spot_localization(spot_id, language, name, category_label, description, business_hours)
    PRIMARY KEY (spot_id, language)
```

API は `lang` を受けて**解決済みの localized Spot を返す**（要求言語が無ければ `ja` フォールバック＝モックの `resolveLocalization` と同じ）。

### 7.2 位置情報（geo）

- **当面：`latitude` / `longitude` を素の数値カラムで保持**（ピン登録はこれで足りる。H2 テストもそのまま動く）
- **最寄り検索（避難所「現在地から近い順」等）が必要になった時点で** PostGIS + Hibernate Spatial を導入し、`geography(Point)` + 空間インデックスで `ST_DWithin` / KNN を使う
- 導入時、空間クエリを含むテストは H2 では不可なので Testcontainers（postgis イメージ）へ切替

### 7.3 評価（rating）

`Spot.rating` はレビュー（feature ②）の平均から算出する派生値。feature ① 時点では `null`（UI 側で非表示）。

---

## 8. REST API 契約（モック準拠）

ベース URL は `/api/v1`。言語は `?lang=` 主・`Accept-Language` 従。

```
# 一覧（モックの fetchSpots(language) に対応）
GET /api/v1/tourism/spots?lang=ja
200 → [{ id, name, genre, description,
         coordinates:{latitude,longitude},
         businessHours, category:{label},
         media:{imageUrl}, rating?:{value} }]

# ピン登録（feature ①。モックには無い backend 初の書き込み）
POST /api/v1/tourism/spots
body → { genre, coordinates:{latitude,longitude}, imageUrl,
         localizations: { ja:{name,categoryLabel,description,businessHours}, en:{...}, ... } }
201 → 作成された Spot
```

この契約を守れば、フロントは `infrastructure/api/mock-spots.ts` を実 fetch に差し替えるだけで application / ui 層を無改修にできる（モックのコメントの想定どおり）。画像 URL は将来 backend が完全 URL を返し、フロントの `buildSpotImageUrl` を引退させる方向。

経路探索（道なり）は当面フロントの OpenRouteService 直叩きのまま。backend はデータ供給に集中する。

---

## 9. 実装優先度

| 順 | 機能 | backend で作るもの | 認証 |
|---|---|---|---|
| ① | **ピン登録**（tourism） | `Spot` 集約 + `spot_localization`。`POST /tourism/spots`（登録）/ `GET …/spots?lang=`（一覧） | なしで先行 |
| ② | **レビュー**（Client #129） | `Review`（rating 1-5・comment・authorName/icon・createdAt・language）。`POST /spots/{id}/reviews`・`GET ?lang=`（言語絞り込み）・`PUT`（自分の編集）。`Spot.rating` はレビュー平均を集計で返す | 仮（authorName を受ける） |
| ③ | **ログイン** | `User` コンテキスト。②の投稿者を本人性で担保。方式は別 issue（#129 メモ：投稿時のみ認証） | 本実装 |

---

## 10. 認証（未確定）

フロント仕様でも「認証未確定」。初期は**認証なし**で進め、③で導入する。それまでは投稿者を引数（`authorName`）で受ける薄い seam を置き、後から本実装に差し替えられる形にする（YAGNI）。

---

## 関連ドキュメント

- Specification `01_overview.md` — プロダクト概要
- Specification `03_architecture.md` — フロントのアーキテクチャ思想
- Specification `07_bounded-contexts.md` — 境界づけられたコンテキスト
- Client `src/features/tourism/` — モック API（`infrastructure/api/mock-spots.ts`）・ドメインモデル（`domain/spot.ts`）の参照実装。**backend domain 設計の正**
- Client Issue #129 — レビュー投稿の受け入れ条件
