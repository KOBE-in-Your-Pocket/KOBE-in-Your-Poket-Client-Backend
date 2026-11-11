-- 天気（降水強度）（#231 / PBI #226）。
--
-- backend のバッチ（#232）が 15 分ごとに Yahoo! 気象情報API から取得した降水強度を保存し、
-- GET /api/v1/weather（#233）で Client に返す。
--
-- 1 行 = ある地点の、ある時刻の降水強度（今の観測値 1 件＋60 分先までの予測値 6 件）。
-- 地点ごとに最新の取得分だけを残す（取得のたびに、その地点の行を消して入れ直す）。
-- API は最新しか返さず、履歴の使い道が無いため。対象地点は今は神戸の中心 1 地点（約 7 行）。

CREATE TABLE weather_readings (
    -- 地点の緯度・経度（WGS84）
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    -- その値がいつの降水強度か
    target_at     TIMESTAMPTZ      NOT NULL,
    -- observation（観測値）/ forecast（予測値）
    reading_type  VARCHAR(16)      NOT NULL,
    -- 降水強度（mm/h）。気象レーダーの値を時間雨量に換算したもので、実際の雨量とは異なる
    mm_per_hour   DOUBLE PRECISION NOT NULL,
    -- backend が Yahoo! から取得した時刻（同じ取得分の行はすべて同じ値）
    fetched_at    TIMESTAMPTZ      NOT NULL,
    -- 1 地点・1 時刻に 1 行
    CONSTRAINT pk_weather_readings PRIMARY KEY (latitude, longitude, target_at),
    CONSTRAINT ck_weather_readings_type CHECK (reading_type IN ('observation', 'forecast')),
    CONSTRAINT ck_weather_readings_mm_per_hour CHECK (mm_per_hour >= 0)
);

-- 新規テーブルは RLS を有効にする（#117）。ポリシーは作らないため、Supabase Data API
-- （anon / authenticated ロール）からは読み書きできない。
ALTER TABLE weather_readings ENABLE ROW LEVEL SECURITY;
