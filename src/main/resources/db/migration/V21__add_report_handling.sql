-- 通報の対応状況（#145）。運営が通報を「承認」「拒否」にでき、誰がいつ対応したかを残す。
--
-- V20 では status を OPEN（受付）だけに絞っていた。ここで APPROVED（承認・通報を認めて口コミを削除）と
-- REJECTED（拒否・問題なし）を足す。管理画面のラベル（承認済み / 拒否済み）に合わせた名前。対応済みを未対応に戻す操作は持たない。

ALTER TABLE reports DROP CONSTRAINT ck_reports_status;
ALTER TABLE reports
    ADD CONSTRAINT ck_reports_status CHECK (status IN ('OPEN', 'APPROVED', 'REJECTED'));

-- 対応した運営（Supabase Auth の user id）と対応日時。OPEN の間は両方 NULL。
-- 運営が退会しても履歴として残すため、users への外部キーは張らない。
ALTER TABLE reports
    ADD COLUMN handled_by UUID,
    ADD COLUMN handled_at TIMESTAMPTZ;

ALTER TABLE reports
    ADD CONSTRAINT ck_reports_handled CHECK (
        (status = 'OPEN' AND handled_by IS NULL AND handled_at IS NULL)
        OR (status <> 'OPEN' AND handled_by IS NOT NULL AND handled_at IS NOT NULL)
    );

COMMENT ON COLUMN reports.handled_by IS
    '対応した運営の user id（Supabase Auth）。OPEN の間は NULL。運営の退会後も履歴として残す。';

-- 運営向け一覧は対象（口コミ）ごとにまとめて並べる。一意制約
-- uq_reports_target_reporter (target_type, target_id, reporter_user_id) の先頭 2 列が
-- そのまま使えるため、対象ごとの索引は追加しない。
