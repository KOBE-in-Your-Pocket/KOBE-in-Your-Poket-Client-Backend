-- #153: spot.genre をジャンルマスタへの参照にする。
--
-- V14 で既存の 5 ジャンルを投入済みのため、現行データはこの制約を満たす。
--
-- ON DELETE は指定しない（既定の NO ACTION）。使用中のジャンルを消せなくすることが
-- 目的で、消したら参照元のスポットがジャンル不明になる。application 層でも件数を見て
-- 409 を返すが、DB 側でも止める（アプリを経由しない操作や、判定漏れへの保険）。
--
-- 更新も NO ACTION。code は不変で、更新 API も書き換えない。

ALTER TABLE spot
    ADD CONSTRAINT fk_spot_genre FOREIGN KEY (genre) REFERENCES genre (code);

-- 削除時の参照チェックと、ジャンル別のスポット件数集計で使う
CREATE INDEX idx_spot_genre ON spot (genre);
