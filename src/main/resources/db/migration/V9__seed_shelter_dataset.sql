-- #66: 神戸市オープンデータ「神戸市避難場所」（CC BY 2.1 JP）から避難所データを投入する。
--
-- 出典: 神戸市オープンデータポータル「神戸市避難場所」
--       https://catalog.city.kobe.lg.jp/dataset/evacuation
--       （緊急避難場所・避難所CSV: https://www2.wagmap.jp/kobecity/kobecity/opendata/map_999/CSV/opendata_1304.csv）
-- ライセンス: クリエイティブ・コモンズ 表示 2.1 日本（CC BY 2.1 JP）。出典表記は必須（README 参照）。
-- データ基準日（最終更新）: 2025-04-02
--
-- 全市の避難場所は数百件規模のため、本マイグレーションでは種別・カテゴリの
-- 多様性を確保できる代表 11 件（学校4・体育館3・公園3・生涯学習支援センター1）を
-- 東灘区・須磨区・垂水区・中央区から選定して投入する。全件取込は将来、
-- 取込スクリプト化する際に改めて検討する（本 issue #66 の「やること」参照）。
--
-- CSV の「避難所としての利用」列が ○ の行は指定避難所を兼ねるため type=dual-use、
-- 空欄（屋外の緊急避難場所のみ）は type=designated-emergency-evacuation-site とした。
-- CSV にバリアフリー（車椅子等アクセシビリティ）情報が無いため accessible は
-- 一律 false とし、実データが確認でき次第 UPDATE で見直す想定。
-- capacity・external_url も CSV に無いため NULL のまま登録する。
-- image_url は実画像が未調達のため spot（V4）と同じ方針でプレースホルダー URL を格納する。

INSERT INTO shelter_dataset_metadata (id, source, as_of) VALUES
    (1, '神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP) https://catalog.city.kobe.lg.jp/dataset/evacuation', DATE '2025-04-02');

-- 東灘小学校
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('higashinada-elementary-school', 34.7248160999997, 135.2944292, 'dual-use', 'school',
     'https://images.kobe-pocket.example.com/shelters/higashinada-elementary-school/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('higashinada-elementary-school', 'ja', '東灘小学校', '神戸市東灘区深江北町2-4-1'),
    ('higashinada-elementary-school', 'en', 'Higashinada Elementary School', '2-4-1 Fukaekitamachi, Higashinada Ward, Kobe'),
    ('higashinada-elementary-school', 'zh', '东滩小学', '神户市东滩区深江北町2-4-1'),
    ('higashinada-elementary-school', 'ko', '히가시나다 초등학교', '고베시 히가시나다구 후카에키타마치 2-4-1');

-- 本庄小学校
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('honjo-elementary-school', 34.7210202199997, 135.2886997, 'dual-use', 'school',
     'https://images.kobe-pocket.example.com/shelters/honjo-elementary-school/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('honjo-elementary-school', 'ja', '本庄小学校', '神戸市東灘区青木4-4-1'),
    ('honjo-elementary-school', 'en', 'Honjo Elementary School', '4-4-1 Aoki, Higashinada Ward, Kobe'),
    ('honjo-elementary-school', 'zh', '本庄小学', '神户市东滩区青木4-4-1'),
    ('honjo-elementary-school', 'ko', '혼조 초등학교', '고베시 히가시나다구 아오키 4-4-1');

-- 本庄中学校
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('honjo-junior-high-school', 34.7204612299997, 135.2875439, 'dual-use', 'school',
     'https://images.kobe-pocket.example.com/shelters/honjo-junior-high-school/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('honjo-junior-high-school', 'ja', '本庄中学校', '神戸市東灘区青木4-4-2'),
    ('honjo-junior-high-school', 'en', 'Honjo Junior High School', '4-4-2 Aoki, Higashinada Ward, Kobe'),
    ('honjo-junior-high-school', 'zh', '本庄中学', '神户市东滩区青木4-4-2'),
    ('honjo-junior-high-school', 'ko', '혼조 중학교', '고베시 히가시나다구 아오키 4-4-2');

-- 東灘高校
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('higashinada-high-school', 34.7099788999997, 135.3010486, 'dual-use', 'school',
     'https://images.kobe-pocket.example.com/shelters/higashinada-high-school/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('higashinada-high-school', 'ja', '東灘高校', '神戸市東灘区深江浜町50'),
    ('higashinada-high-school', 'en', 'Higashinada High School', '50 Fukaehamamachi, Higashinada Ward, Kobe'),
    ('higashinada-high-school', 'zh', '东滩高中', '神户市东滩区深江浜町50'),
    ('higashinada-high-school', 'ko', '히가시나다 고등학교', '고베시 히가시나다구 후카에하마마치 50');

-- 東灘体育館
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('higashinada-gymnasium', 34.7146659999997, 135.278116, 'dual-use', 'gymnasium',
     'https://images.kobe-pocket.example.com/shelters/higashinada-gymnasium/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('higashinada-gymnasium', 'ja', '東灘体育館', '神戸市東灘区魚崎南町6-5-11'),
    ('higashinada-gymnasium', 'en', 'Higashinada Gymnasium', '6-5-11 Uozakiminamimachi, Higashinada Ward, Kobe'),
    ('higashinada-gymnasium', 'zh', '东滩体育馆', '神户市东滩区鱼崎南町6-5-11'),
    ('higashinada-gymnasium', 'ko', '히가시나다 체육관', '고베시 히가시나다구 우오자키미나미마치 6-5-11');

-- 須磨体育館
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('suma-gymnasium', 34.6507527999997, 135.1289669, 'dual-use', 'gymnasium',
     'https://images.kobe-pocket.example.com/shelters/suma-gymnasium/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('suma-gymnasium', 'ja', '須磨体育館', '神戸市須磨区中島町1-2-2'),
    ('suma-gymnasium', 'en', 'Suma Gymnasium', '1-2-2 Nakajimacho, Suma Ward, Kobe'),
    ('suma-gymnasium', 'zh', '须磨体育馆', '神户市须磨区中岛町1-2-2'),
    ('suma-gymnasium', 'ko', '스마 체육관', '고베시 스마구 나카지마초 1-2-2');

-- 垂水体育館
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('tarumi-gymnasium', 34.6262784899996, 135.0601363, 'dual-use', 'gymnasium',
     'https://images.kobe-pocket.example.com/shelters/tarumi-gymnasium/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('tarumi-gymnasium', 'ja', '垂水体育館', '神戸市垂水区平磯1-1-56'),
    ('tarumi-gymnasium', 'en', 'Tarumi Gymnasium', '1-1-56 Hiraiso, Tarumi Ward, Kobe'),
    ('tarumi-gymnasium', 'zh', '垂水体育馆', '神户市垂水区平矶1-1-56'),
    ('tarumi-gymnasium', 'ko', '다루미 체육관', '고베시 다루미구 히라이소 1-1-56');

-- 本庄中央公園（屋外の緊急避難場所のみ）
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('honjo-central-park', 34.7195557999997, 135.2850366, 'designated-emergency-evacuation-site', 'park',
     'https://images.kobe-pocket.example.com/shelters/honjo-central-park/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('honjo-central-park', 'ja', '本庄中央公園', '神戸市東灘区青木5-18'),
    ('honjo-central-park', 'en', 'Honjo Central Park', '5-18 Aoki, Higashinada Ward, Kobe'),
    ('honjo-central-park', 'zh', '本庄中央公园', '神户市东滩区青木5-18'),
    ('honjo-central-park', 'ko', '혼조 중앙공원', '고베시 히가시나다구 아오키 5-18');

-- 住吉宮町公園（屋外の緊急避難場所のみ）
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('sumiyoshi-miyamachi-park', 34.7167953299997, 135.2635727, 'designated-emergency-evacuation-site', 'park',
     'https://images.kobe-pocket.example.com/shelters/sumiyoshi-miyamachi-park/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('sumiyoshi-miyamachi-park', 'ja', '住吉宮町公園', '神戸市東灘区住吉宮町3-2'),
    ('sumiyoshi-miyamachi-park', 'en', 'Sumiyoshi-Miyamachi Park', '3-2 Sumiyoshimiyamachi, Higashinada Ward, Kobe'),
    ('sumiyoshi-miyamachi-park', 'zh', '住吉宫町公园', '神户市东滩区住吉宫町3-2'),
    ('sumiyoshi-miyamachi-park', 'ko', '스미요시미야마치 공원', '고베시 히가시나다구 스미요시미야마치 3-2');

-- 住吉公園（屋外の緊急避難場所のみ）
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('sumiyoshi-park', 34.7162363999997, 135.2624276, 'designated-emergency-evacuation-site', 'park',
     'https://images.kobe-pocket.example.com/shelters/sumiyoshi-park/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('sumiyoshi-park', 'ja', '住吉公園', '神戸市東灘区住吉宮町3-3'),
    ('sumiyoshi-park', 'en', 'Sumiyoshi Park', '3-3 Sumiyoshimiyamachi, Higashinada Ward, Kobe'),
    ('sumiyoshi-park', 'zh', '住吉公园', '神户市东滩区住吉宫町3-3'),
    ('sumiyoshi-park', 'ko', '스미요시 공원', '고베시 히가시나다구 스미요시미야마치 3-3');

-- コミスタこうべ（生涯学習支援センター）
INSERT INTO shelter (id, latitude, longitude, type, facility_category, image_url, accessible) VALUES
    ('comista-kobe-lifelong-learning-center', 34.6982260999997, 135.2046319, 'dual-use', 'government',
     'https://images.kobe-pocket.example.com/shelters/comista-kobe-lifelong-learning-center/main.jpg', false);
INSERT INTO shelter_localization (shelter_id, language, name, address) VALUES
    ('comista-kobe-lifelong-learning-center', 'ja', 'コミスタこうべ（生涯学習支援センター）', '神戸市中央区吾妻通4-1-6'),
    ('comista-kobe-lifelong-learning-center', 'en', 'Comista Kobe (Lifelong Learning Support Center)', '4-1-6 Azumadori, Chuo Ward, Kobe'),
    ('comista-kobe-lifelong-learning-center', 'zh', '神户市终身学习支援中心（Comista神户）', '神户市中央区吾妻通4-1-6'),
    ('comista-kobe-lifelong-learning-center', 'ko', '코미스타 고베(평생학습지원센터)', '고베시 주오구 아즈마도리 4-1-6');
