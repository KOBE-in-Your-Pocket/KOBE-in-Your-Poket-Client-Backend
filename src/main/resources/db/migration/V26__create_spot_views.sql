-- 観光スポットの閲覧（#239 / PBI #228）。Client で観光スポットのカードから詳細画面に遷移した 1 回 = 1 行。
--
-- 閲覧数（合計）ではなく閲覧 1 回ごとに記録する。
-- - 記録は INSERT だけなので、同時に閲覧されても数がずれない（同じ行の数値を更新し合わない。#240）
-- - viewed_at があるので、合計のほかに日別・期間別の集計もあとから出せる
-- - 重複・連打の扱い（#242）が決まったら、同じ時間内の記録を数えないといった絞り込みもできる
-- 集計（合計）は COUNT で行う。行が増えて重くなったら、日別の集計テーブルを別に作る。
--
-- spot へは外部キーを張り、スポットの削除に合わせて消す（favorites と同じ。同じ Tourism コンテキスト内の参照のため）。
-- 未ログインでも数えるため、利用者は持たない。

CREATE TABLE spot_views (
    -- 1 回の閲覧を区別するための識別子（backend で採番）
    id        UUID         NOT NULL,
    -- Tourism の Spot.id
    spot_id   VARCHAR(128) NOT NULL,
    -- backend が記録した閲覧日時
    viewed_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_spot_views PRIMARY KEY (id),
    CONSTRAINT fk_spot_views_spot FOREIGN KEY (spot_id) REFERENCES spot (id) ON DELETE CASCADE
);

-- スポット別の合計（spot_id）と、日別・期間別の集計（spot_id + viewed_at）の両方に効かせる。
CREATE INDEX idx_spot_views_spot_id_viewed_at ON spot_views (spot_id, viewed_at);

-- 新規テーブルは RLS を有効にする（#117）。ポリシーは作らないため、Supabase Data API
-- （anon / authenticated ロール）からは読み書きできない。
ALTER TABLE spot_views ENABLE ROW LEVEL SECURITY;
