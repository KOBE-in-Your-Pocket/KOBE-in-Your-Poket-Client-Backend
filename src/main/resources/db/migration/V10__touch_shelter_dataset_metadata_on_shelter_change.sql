-- #85: 避難所データセットのバージョン（= shelter_dataset_metadata.updated_at）を
-- 「シード更新時に自動で上がる」仕組みにする。
--
-- 運用者が shelter / shelter_localization を変更するたびに
-- shelter_dataset_metadata.updated_at を手動で更新するのは忘れやすいため、
-- トリガーで自動化する（Client はこの updated_at を起動時の差分チェックキーに使う）。
-- 行単位の情報は不要なため STATEMENT レベルトリガーで十分。
--
-- now() はトランザクション開始時刻を返すため、開始が早いトランザクションほど
-- コミットが遅れた場合に updated_at が過去へ巻き戻りうる（版が単調増加しなくなる）。
-- 実行時刻を返す clock_timestamp() を使い、単調増加を保証する。

CREATE FUNCTION touch_shelter_dataset_metadata() RETURNS TRIGGER AS $$
BEGIN
    UPDATE shelter_dataset_metadata SET updated_at = clock_timestamp() WHERE id = 1;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_shelter_touch_dataset_metadata
    AFTER INSERT OR UPDATE OR DELETE ON shelter
    FOR EACH STATEMENT
    EXECUTE FUNCTION touch_shelter_dataset_metadata();

CREATE TRIGGER trg_shelter_localization_touch_dataset_metadata
    AFTER INSERT OR UPDATE OR DELETE ON shelter_localization
    FOR EACH STATEMENT
    EXECUTE FUNCTION touch_shelter_dataset_metadata();
