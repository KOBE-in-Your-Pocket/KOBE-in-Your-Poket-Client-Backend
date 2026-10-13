# 通報（#147）

利用者が不適切なレビューを運営へ知らせる仕組み。App Store Guideline 1.2（UGC）を満たして
レビュー投稿を解禁する（Client #527 / #556）ための前提になる。

## API

`POST /api/v1/tourism/spots/{spotId}/reviews/{reviewId}/reports`（認証必須・一般ロール可）

```json
{ "reason": "OTHER", "description": "別のスポットの話をしている" }
```

| status | 条件 |
| --- | --- |
| 201 | 受け付けた（`status: "OPEN"` の通報を返す） |
| 400 | `reason` が不正 / `OTHER` で `description` が空 / `description` が 500 文字超 / 自分のレビュー |
| 401 | 未ログイン |
| 404 | レビューが無い / パスの `spotId` 配下にない |
| 409 | 同じ人が同じレビューをすでに通報している |

### 通報理由（`reason`）

Client のラジオボタンの選択肢に 1 対 1 で対応する。表示文言（4 言語）は Client が持ち、API は code だけを扱う。

| code | 日本語ラベル |
| --- | --- |
| `SPAM` | スパム・宣伝目的 |
| `HARASSMENT` | 誹謗中傷・嫌がらせ |
| `HATE` | 差別的・ヘイト表現 |
| `SEXUAL_OR_VIOLENT` | 性的・暴力的な内容 |
| `PERSONAL_INFO` | 個人情報の掲載 |
| `MISLEADING` | 虚偽・スポットと無関係な内容 |
| `OTHER` | その他（`description` 必須） |

選択肢を増やすときは `ReportReason` と DB の CHECK 制約（`ck_reports_reason`）を新しいマイグレーションで一緒に広げる。

## データ（`reports` / V20・V21）

- 対象は `target_type` + `target_id` で汎用に持つ。現状は `REVIEW` のみ。スポット・避難所は #145 の残りで追加する
- `(target_type, target_id, reporter_user_id)` の一意制約で重複通報を防ぐ
- 対象への外部キーは張らない。**レビューが削除されても通報は残す**（運営の対応履歴）
- **通報者が退会したら本人の通報は削除する**（`DeleteUserService`）。退会者のレビューに他人が送った通報は残る
- 対応状況（V21）: `status` は `OPEN`（未対応）→ `APPROVED`（承認済み）/ `REJECTED`（拒否済み）へ一度だけ進む。
  対応した運営の id（`handled_by`）と日時（`handled_at`）を残す
- RLS 有効・ポリシーなし（#117）。Supabase Data API からは読み書きできない

## 運営向け API（#145）

いずれも OPERATOR 以上（ADMIN も可）。通報者の id と名前を含むため公開しない。

### 一覧 `GET /api/v1/reports/reviews`

通報を**口コミごとにまとめて**、未対応の通報が多い順（同数なら最新の通報が新しい順）に返す。

| パラメータ | 説明 |
| --- | --- |
| `status` | `OPEN` / `APPROVED` / `REJECTED`。その状態の通報が 1 件以上ある口コミだけに絞る。省略で全件 |
| `page` / `size` | 0 始まり。`size` は既定 20・上限 200（他の運営向け一覧と同じ / #205） |
| `lang` | スポット名の解決にだけ効く。本文・投稿者名は投稿時の言語のまま |

各口コミには、口コミの本文・評価・投稿者・スポット名（削除済みなら `review: null`）、件数（`reportCount` / `openCount`）、
理由ごとの件数（`reasonCounts`）、通報の明細（理由・自由記述・状態・通報者の id と名前・対応者・対応日時）が入る。
件数と明細は `status` の絞り込みにかかわらず、その口コミの全通報から作る。

### 対応 `PATCH /api/v1/reports/reviews/{reviewId}`

```json
{ "status": "APPROVED" }
```

その口コミへの**未対応の通報をまとめて** `APPROVED` か `REJECTED` にする。対応済みの通報は書き換えない。

| status | 条件 |
| --- | --- |
| 200 | 更新した（`updatedCount`。全件対応済みなら 0） |
| 400 | `status` が不正、または `OPEN` |
| 404 | その口コミへの通報が 1 件も無い |

## 運営フロー

通報されたレビューは**自動では非表示にしない**。運営が管理画面（ADMIN の通報画面）で確認して判断する。

| 判断 | 操作 | 管理画面 | アプリ |
| --- | --- | --- | --- |
| 承認（問題あり） | PATCH `APPROVED` | 口コミは残る | **非表示**（下記） |
| 拒否（問題なし） | PATCH `REJECTED` | 口コミは残る | 表示のまま |
| 拒否後にやはり消す | `DELETE /api/v1/tourism/reviews/{reviewId}` | 削除済み | 消える |

- **承認しても口コミは消さない。** 一般向けのレビュー取得（`GET /api/v1/tourism/spots/{spotId}/reviews`）が、
  承認済みの通報がある口コミに `hiddenByReport: true` を付けて返し、Client がそれを見て非表示にする（#202）
- 運営がモデレーション削除（`DELETE /api/v1/tourism/reviews/{reviewId}`）すると、その口コミへの未対応の通報は
  削除した運営の対応として `APPROVED` になる。拒否済みの通報は書き換えない
- 投稿者本人の削除・退会で口コミが消えた場合、未対応の通報は残る。一覧で `review: null` の行として出るので、運営が閉じる

### 既知の制限

- 承認した口コミの本文は、一般向けのレスポンスに入ったまま届く。`hiddenByReport` を知らない古い Client では表示される
- スポットの平均評価には、承認した口コミの評価も含まれたまま

Guideline 1.2 は不適切なコンテンツへの速やかな対応を求めている（Client の
`docs/appstore-release-plan.md` では 24 時間以内を目安にしている）。通知（メール転送など）は
スコープ外のため、レビュー投稿の解禁後は運営が定期的に管理画面の「未対応」を確認する。
