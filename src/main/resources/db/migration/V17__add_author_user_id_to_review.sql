-- 投稿者本人によるレビュー削除・編集（#86）のため、投稿者の user id を保持する。
--
-- V2 時点では PublicUser 仕様が未確定で author_name / author_icon_url のみを持っていた。
-- 表示名は一意でも不変でもないため「誰の投稿か」の判定には使えず、本人判定ができなかった。
--
-- 既存行は投稿者を特定できないため NULL のままにする（NOT NULL にはできない）。
-- NULL の行は本人削除・本人編集の対象外となり、運営のモデレーション削除でのみ消せる。
ALTER TABLE review
    ADD COLUMN author_user_id UUID;

COMMENT ON COLUMN review.author_user_id IS
    'ReviewAuthor.userId（Supabase Auth の user id）。V17 以前の投稿は NULL で本人操作の対象外。';

-- 「自分の投稿一覧」は現時点で無いが、本人判定は削除・編集のたびに走る。
-- 主キー引きの後に列を比較するだけなので索引は不要だが、将来の投稿者別検索に備えて
-- NULL を含まない部分索引だけ張っておく（既存行が NULL 主体のため部分索引が効率的）。
CREATE INDEX idx_review_author_user_id
    ON review (author_user_id)
    WHERE author_user_id IS NOT NULL;
