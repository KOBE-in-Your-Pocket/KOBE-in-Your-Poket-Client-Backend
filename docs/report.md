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

## データ（`reports` / V20）

- 対象は `target_type` + `target_id` で汎用に持つ。現状は `REVIEW` のみ。スポット・避難所は #145 で追加する
- `(target_type, target_id, reporter_user_id)` の一意制約で重複通報を防ぐ
- 対象への外部キーは張らない。**レビューが削除されても通報は残す**（運営の対応履歴）
- **通報者が退会したら本人の通報は削除する**（`DeleteUserService`）。退会者のレビューに他人が送った通報は残る
- RLS 有効・ポリシーなし（#117）。Supabase Data API からは読み書きできない

## 運営フロー

通報されたレビューは**自動では非表示にしない**。運営が確認して判断する。

1. **確認**: 運営向け一覧 API は未実装（#145）。それまでは Supabase の SQL Editor で未対応の通報を見る

   ```sql
   SELECT r.created_at, r.reason, r.description, r.target_id AS review_id,
          v.spot_id, v.comment, v.author_name
   FROM reports r
   LEFT JOIN review v ON v.id::text = r.target_id
   WHERE r.target_type = 'REVIEW' AND r.status = 'OPEN'
   ORDER BY r.created_at DESC;
   ```

   `v.*` が NULL の行は、レビューがすでに削除されている。

2. **判断と削除**: 不適切なら運営のモデレーション削除で消す
   `DELETE /api/v1/tourism/reviews/{reviewId}`（OPERATOR 以上 / #165）。投稿者本人かどうかは問わない
3. **対応状況の記録**: `status` は現状 `OPEN` のみ。対応済み・却下の更新は #145 で運営向け一覧と合わせて追加する

Guideline 1.2 は不適切なコンテンツへの速やかな対応を求めている（Client の
`docs/appstore-release-plan.md` では 24 時間以内を目安にしている）。通知（メール転送など）は
スコープ外のため、レビュー投稿の解禁後は運営が定期的に上の SQL で確認する。
