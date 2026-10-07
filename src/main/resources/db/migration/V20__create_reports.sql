-- 通報（#147 / #145）。利用者が不適切なコンテンツを運営へ知らせた記録を保持する。
--
-- 対象は target_type + target_id で汎用に表す。現状はレビュー（REVIEW）のみで、
-- スポット・避難所などは #145 で CHECK 制約を広げて追加する。
-- target_id は対象ごとに ID の形式が違う（レビューは UUID、スポットは slug）ため文字列で持つ。
--
-- 対象への外部キーは張らない。対象が削除されても、運営の対応履歴として通報は残す。
-- 通報者が退会したときは、アプリ側の退会処理で本人の通報を削除する（個人データを残さない）。

CREATE TABLE reports (
    id               UUID         NOT NULL,
    -- ReportTarget.Type
    target_type      VARCHAR(16)  NOT NULL,
    -- ReportTarget.id（最大 128 文字）
    target_id        VARCHAR(128) NOT NULL,
    -- 通報者（Supabase Auth の user id / JWT sub）
    reporter_user_id UUID         NOT NULL,
    -- ReportReason（Client のラジオボタンの選択肢）
    reason           VARCHAR(32)  NOT NULL,
    -- 自由記述（最大 500 文字）。reason = OTHER のときは必須
    description      TEXT,
    -- ReportStatus。運営の対応状況（現状は受付のみ）
    status           VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    -- domain 側で採番した通報日時をそのまま保存
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_reports PRIMARY KEY (id),
    -- 同じ人が同じ対象を 2 回通報できない（重複通報は 409）
    CONSTRAINT uq_reports_target_reporter UNIQUE (target_type, target_id, reporter_user_id),
    CONSTRAINT ck_reports_target_type CHECK (target_type IN ('REVIEW')),
    CONSTRAINT ck_reports_reason CHECK (
        reason IN ('SPAM', 'HARASSMENT', 'HATE', 'SEXUAL_OR_VIOLENT', 'PERSONAL_INFO', 'MISLEADING', 'OTHER')
    ),
    CONSTRAINT ck_reports_status CHECK (status IN ('OPEN')),
    CONSTRAINT ck_reports_description_length CHECK (char_length(description) <= 500),
    CONSTRAINT ck_reports_description_not_blank CHECK (description IS NULL OR char_length(trim(description)) > 0),
    CONSTRAINT ck_reports_other_requires_description CHECK (reason <> 'OTHER' OR description IS NOT NULL)
);

-- 退会時に本人の通報をまとめて消すため。
CREATE INDEX idx_reports_reporter_user_id ON reports (reporter_user_id);

-- 運営向け一覧（#145）は未対応のものを新しい順に見る想定。
CREATE INDEX idx_reports_status_created_at ON reports (status, created_at DESC);

-- 新規テーブルは RLS を有効にする（#117）。ポリシーは作らないため、Supabase Data API
-- （anon / authenticated ロール）からは読み書きできない。バックエンドは Flyway と同じ
-- 接続ユーザー（＝テーブル所有者）で動くため、FORCE しない限り RLS の影響を受けない。
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
