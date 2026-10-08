-- 通報の対応状況 RESOLVED（対応済み）を APPROVED（承認）へ改名する。
--
-- RESOLVED は「運営が通報を認め、口コミを利用規約違反としてアプリ全体での非表示に同意した」
-- という意味で使っていたが、「対応済み」では却下（DISMISSED）も含むように読めて紛らわしい。
-- レビュー取得の hiddenByReport はこの値を見て決まる。

ALTER TABLE reports DROP CONSTRAINT ck_reports_status;

UPDATE reports SET status = 'APPROVED' WHERE status = 'RESOLVED';

ALTER TABLE reports
    ADD CONSTRAINT ck_reports_status CHECK (status IN ('OPEN', 'APPROVED', 'DISMISSED'));
