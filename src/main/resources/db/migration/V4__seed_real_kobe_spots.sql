-- #62: 実スポット seed データ（神戸の実在観光地16件、ja/en/zh/ko 4言語ローカライズ）を投入する。
-- モックの `mock-spots`（Client 側）を置き換える実データの初回セットで、Client の
-- mock-spots 廃止（KOBE-in-Your-Pocket/KOBE-in-Your-Poket-Client#305）に先立ち整備する。
--
-- image_url は要件定義 O-2（Client `buildSpotImageUrl` 引退）に合わせて完全 URL 形式で格納する。
-- 以下 5 件（kobe-port-tower / kitano-ijinkan / nankinmachi / arima-onsen / mount-rokko）は
-- Client `mock-spots.ts` の `S3_SPOT_IMAGE_FILES` で既に実画像がアップロード済みのため、
-- Client `.env` の `EXPO_PUBLIC_S3_IMAGE_BASE_URL`（S3 バケット）を基点にした実 URL を格納する。
-- 座標・4言語ローカライズも Client `mock-spots.ts` / `mock-spot-localizations.ts` の実データをそのまま踏襲する。
-- それ以外のスポットは実画像（権利処理済み）が未調達のため、
-- `https://images.kobe-pocket.example.com/spots/{id}/main.jpg` のプレースホルダー URL を暫定で格納する。
-- 実 CDN/S3 URL が確定し次第、UPDATE で差し替える想定。
--
-- rating_value は feature ②（レビュー）由来の派生値のため、V1 の方針どおり NULL のまま登録する。

-- 神戸ポートタワー（Client mock-spots 実データ + 実S3画像）
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-port-tower', 'landmark', 34.6826, 135.1863, 'https://kobe-in-your-pocket-images-dev-515966496540.s3.ap-northeast-1.amazonaws.com/spots/kobe-port-tower/main.webp');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-port-tower', 'ja', '神戸ポートタワー', 'ウォーターフロント', '神戸港のシンボル。鼓を思わせる赤い鉄塔で、展望フロアから街と海を一望できる。', '9:00 – 21:00', '神戸市中央区波止場町5-5'),
    ('kobe-port-tower', 'en', 'Kobe Port Tower', 'Waterfront', 'A symbol of Kobe Port. This red lattice tower resembles a drum and offers panoramic views of the city and sea from its observation deck.', '9:00 AM – 9:00 PM', '5-5 Hatobacho, Chuo Ward, Kobe'),
    ('kobe-port-tower', 'zh', '神户港塔', '海滨', '神户港的象征。形如鼓的红色铁塔，从展望层可一览城市与大海。', '9:00 – 21:00', '神户市中央区波止场町5-5'),
    ('kobe-port-tower', 'ko', '고베 포트 타워', '워터프런트', '고베 항의 상징. 북을 연상시키는 붉은 철탑으로, 전망층에서 도시와 바다를 한눈에 볼 수 있다.', '9:00 – 21:00', '고베시 주오구 하토바초 5-5');

-- 北野異人館街（Client mock-spots 実データ + 実S3画像）
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kitano-ijinkan', 'history', 34.6989, 135.1896, 'https://kobe-in-your-pocket-images-dev-515966496540.s3.ap-northeast-1.amazonaws.com/spots/kitano-ijinkan/main.webp');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kitano-ijinkan', 'ja', '北野異人館街', '歴史地区', '開港期に外国人が暮らした洋館が立ち並ぶ地区。風見鶏の館をはじめ異国情緒あふれる街並みが残る。', '9:00 – 18:00', '神戸市中央区北野町'),
    ('kitano-ijinkan', 'en', 'Kitano Ijinkan District', 'Historic District', 'A neighborhood of Western-style mansions where foreign residents lived after the port opened. Exotic streetscapes remain, including the Weathercock House.', '9:00 AM – 6:00 PM', 'Kitanocho, Chuo Ward, Kobe'),
    ('kitano-ijinkan', 'zh', '北野异人馆街', '历史街区', '开港时期外国人居住的洋馆林立。风见鸡馆等充满异国情调的街景至今保留。', '9:00 – 18:00', '神户市中央区北野町'),
    ('kitano-ijinkan', 'ko', '기타노 이진칸 거리', '역사 지구', '개항기 외국인들이 살던 양관이 늘어선 지역. 풍향계관을 비롯해 이국적인 거리 풍경이 남아 있다.', '9:00 – 18:00', '고베시 주오구 기타노초');

-- 南京町（Client mock-spots 実データ + 実S3画像）
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('nankinmachi', 'gourmet', 34.6889, 135.1877, 'https://kobe-in-your-pocket-images-dev-515966496540.s3.ap-northeast-1.amazonaws.com/spots/nankinmachi/main.webp');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('nankinmachi', 'ja', '南京町', '中華街', '西日本有数の中華街。豚まんや点心など食べ歩きグルメでにぎわう。', '10:00 – 20:00', '神戸市中央区栄町通'),
    ('nankinmachi', 'en', 'Nankinmachi', 'Chinatown', 'One of the largest Chinatowns in western Japan. Bustling with street food such as pork buns and dim sum.', '10:00 AM – 8:00 PM', 'Sakaemachidori, Chuo Ward, Kobe'),
    ('nankinmachi', 'zh', '南京町', '中华街', '西日本屈指可数的中华街。肉包、点心等街头美食令这里热闹非凡。', '10:00 – 20:00', '神户市中央区荣町通'),
    ('nankinmachi', 'ko', '난킨마치', '차이나타운', '서일본 최대급 차이나타운. 돈만과 딤섬 등 길거리 음식으로 붐비는 곳.', '10:00 – 20:00', '고베시 주오구 사카에마치도리');

-- 有馬温泉（Client mock-spots 実データ + 実S3画像）
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('arima-onsen', 'onsen', 34.7956, 135.2468, 'https://kobe-in-your-pocket-images-dev-515966496540.s3.ap-northeast-1.amazonaws.com/spots/arima-onsen/main.webp');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('arima-onsen', 'ja', '有馬温泉', '温泉', '日本三古湯のひとつ。鉄分を含む茶褐色の「金泉」と無色透明の「銀泉」で知られる。', '24時間', '神戸市北区有馬町'),
    ('arima-onsen', 'en', 'Arima Onsen', 'Hot Spring', 'One of Japan’s three oldest hot springs. Famous for its iron-rich brown “Kinsen” and clear “Ginsen” waters.', 'Open 24 hours', 'Arimacho, Kita Ward, Kobe'),
    ('arima-onsen', 'zh', '有马温泉', '温泉', '日本三大古汤之一。以含铁茶褐色的「金泉」与无色透明的「银泉」闻名。', '24小时', '神户市北区有马町'),
    ('arima-onsen', 'ko', '아리마 온천', '온천', '일본 3대 고온천 중 하나. 철분이 함유된 갈색 「금천」과 무색투명한 「은천」으로 유명하다.', '24시간', '고베시 기타구 아리마초');

-- 六甲山（Client mock-spots 実データ + 実S3画像）
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('mount-rokko', 'nature', 34.7488, 135.2231, 'https://kobe-in-your-pocket-images-dev-515966496540.s3.ap-northeast-1.amazonaws.com/spots/mount-rokko/main.webp');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('mount-rokko', 'ja', '六甲山', '自然', '神戸の街を見下ろす山。ハイキングや植物園が楽しめ、山上から望む夜景は日本三大夜景に数えられる。', '9:00 – 17:00', '神戸市灘区六甲山町'),
    ('mount-rokko', 'en', 'Mount Rokko', 'Nature', 'A mountain overlooking Kobe. Enjoy hiking and botanical gardens; the night view from the summit is counted among Japan’s three greatest nightscapes.', '9:00 AM – 5:00 PM', 'Rokkosancho, Nada Ward, Kobe'),
    ('mount-rokko', 'zh', '六甲山', '自然', '俯瞰神户市区的山。可徒步与参观植物园，山顶夜景被誉为日本三大夜景之一。', '9:00 – 17:00', '神户市滩区六甲山町'),
    ('mount-rokko', 'ko', '롯코산', '자연', '고베 시내를 내려다보는 산. 하이킹과 식물원을 즐길 수 있으며, 정상에서 바라보는 야경은 일본 3대 야경에 꼽힌다.', '9:00 – 17:00', '고베시 나다구 롯코산초');

-- メリケンパーク
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('meriken-park', 'landmark', 34.6853, 135.1885, 'https://images.kobe-pocket.example.com/spots/meriken-park/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('meriken-park', 'ja', 'メリケンパーク', 'ウォーターフロント', '神戸港に面したウォーターフロント公園。BE KOBEモニュメントや神戸海洋博物館があり、港町神戸を象徴する景観が広がる。', '入園自由（24時間開放）', '神戸市中央区波止場町2'),
    ('meriken-park', 'en', 'Meriken Park', 'Waterfront', 'A waterfront park facing Kobe Port, home to the BE KOBE monument and the Kobe Maritime Museum, symbolizing the city''s port heritage.', 'Open 24 hours, free admission', '2 Hatobacho, Chuo Ward, Kobe'),
    ('meriken-park', 'zh', '美利坚公园', '海滨', '面向神户港的滨海公园，园内有BE KOBE纪念碑和神户海洋博物馆，展现港口城市神户的象征性景观。', '全天开放，免费入园', '神户市中央区波止场町2'),
    ('meriken-park', 'ko', '메리켄 파크', '워터프런트', '고베항에 면한 워터프런트 공원으로, BE KOBE 모뉴먼트와 고베 해양박물관이 있어 항구 도시 고베를 상징하는 경관을 볼 수 있다.', '24시간 개방, 입장 무료', '고베시 주오구 하토바초 2');

-- 神戸ハーバーランド
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-harborland', 'landmark', 34.6819, 135.1808, 'https://images.kobe-pocket.example.com/spots/kobe-harborland/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-harborland', 'ja', '神戸ハーバーランド', 'ショッピングエリア', '神戸港に面した複合商業エリア。umieやモザイクなどの商業施設が集まり、夜景スポットとしても人気。', '施設により異なる（目安 10:00〜20:00）', '神戸市中央区東川崎町1丁目'),
    ('kobe-harborland', 'en', 'Kobe Harborland', 'Shopping District', 'A waterfront shopping and entertainment district on Kobe Port, home to umie and Mosaic, and a popular spot for night views.', 'Varies by facility (approx. 10:00-20:00)', '1-chome, Higashikawasakicho, Chuo Ward, Kobe'),
    ('kobe-harborland', 'zh', '神户海港乐园', '购物区', '面向神户港的综合商业区，umie、Mosaic等商业设施云集，也是热门的夜景观赏地。', '因设施而异（约10:00-20:00）', '神户市中央区东川崎町1丁目'),
    ('kobe-harborland', 'ko', '고베 하버랜드', '쇼핑 지구', '고베항에 면한 복합 상업지구로, umie와 모자이크 등의 상업시설이 모여 있으며 야경 명소로도 인기가 높다.', '시설마다 다름 (대략 10:00~20:00)', '고베시 주오구 히가시카와사키초 1초메');

-- 布引の滝
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('nunobiki-falls', 'nature', 34.7017, 135.1949, 'https://images.kobe-pocket.example.com/spots/nunobiki-falls/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('nunobiki-falls', 'ja', '布引の滝', '滝', '「日本三大神滝」に数えられる名瀑。雄滝・雌滝など4つの滝の総称で、新神戸駅から徒歩圏内で楽しめる。', '見学自由（散策路は日中の利用を推奨）', '神戸市中央区葺合町布引山'),
    ('nunobiki-falls', 'en', 'Nunobiki Falls', 'Waterfall', 'One of Japan''s three sacred waterfalls, comprising four falls including Otaki and Mentaki, reachable on foot from Shin-Kobe Station.', 'Freely accessible (daytime visits recommended)', 'Nunobikiyama, Fukiai-cho, Chuo Ward, Kobe'),
    ('nunobiki-falls', 'zh', '布引瀑布', '瀑布', '被誉为「日本三大神瀑」之一，由雄泷、雌泷等四条瀑布组成，从新神户站步行即可抵达。', '自由参观（建议白天前往）', '神户市中央区苇合町布引山'),
    ('nunobiki-falls', 'ko', '누노비키 폭포', '폭포', '''일본 3대 신성한 폭포'' 중 하나로 꼽히며 오타키·메타키 등 4개의 폭포로 이루어져 있으며, 신고베역에서 도보로 갈 수 있다.', '자유 관람 (주간 방문 권장)', '고베시 주오구 후키아이초 누노비키야마');

-- 神戸市立王子動物園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('oji-zoo', 'nature', 34.7096, 135.2126, 'https://images.kobe-pocket.example.com/spots/oji-zoo/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('oji-zoo', 'ja', '神戸市立王子動物園', '動物園', '約120種700点の動物と出会える動物園。桜の名所としても知られ、遊園地も併設する。', '9:00〜17:00（11〜2月は16:30まで）、水曜定休', '神戸市灘区王子町3-1'),
    ('oji-zoo', 'en', 'Kobe Municipal Oji Zoo', 'Zoo', 'Home to around 120 species and 700 animals, this zoo is also a popular cherry-blossom spot and includes an amusement park.', '9:00-17:00 (until 16:30 Nov-Feb), closed Wednesdays', '3-1 Ojicho, Nada Ward, Kobe'),
    ('oji-zoo', 'zh', '神户市立王子动物园', '动物园', '可邂逅约120种700只动物的动物园，也是著名的赏樱胜地，园内还设有游乐场。', '9:00-17:00（11月至2月至16:30），周三休园', '神户市滩区王子町3-1'),
    ('oji-zoo', 'ko', '고베시립 오지 동물원', '동물원', '약 120종 700여 마리의 동물을 만날 수 있는 동물원으로, 벚꽃 명소로도 알려져 있으며 놀이공원도 함께 있다.', '9:00~17:00 (11~2월은 16:30까지), 수요일 휴무', '고베시 나다구 오지초 3-1');

-- 摩耶山掬星台
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('mayasan-kikuseidai', 'nature', 34.7204, 135.2278, 'https://images.kobe-pocket.example.com/spots/mayasan-kikuseidai/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('mayasan-kikuseidai', 'ja', '摩耶山掬星台', '夜景', '標高702mから大阪湾までを見渡す展望台。日本三大夜景のひとつに数えられる絶景が広がる。', '見学自由（ロープウェイ運行時間に準ずる）', '神戸市灘区摩耶山町2-2'),
    ('mayasan-kikuseidai', 'en', 'Mt. Maya Kikuseidai', 'Night View', 'An observation deck at 702m elevation overlooking Osaka Bay, celebrated as one of Japan''s three great night views.', 'Freely accessible (subject to ropeway operating hours)', '2-2 Mayasanmachi, Nada Ward, Kobe'),
    ('mayasan-kikuseidai', 'zh', '摩耶山掬星台', '夜景', '海拔702米的展望台，可远眺大阪湾，是日本三大夜景之一的绝美景观。', '自由参观（依缆车运行时间而定）', '神户市滩区摩耶山町2-2'),
    ('mayasan-kikuseidai', 'ko', '마야산 기쿠세이다이', '야경', '해발 702m에서 오사카만까지 내려다보이는 전망대로, 일본 3대 야경 중 하나로 꼽히는 절경을 자랑한다.', '자유 관람 (로프웨이 운행 시간에 준함)', '고베시 나다구 마야산초 2-2');

-- 生田神社
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('ikuta-shrine', 'history', 34.6939, 135.1926, 'https://images.kobe-pocket.example.com/spots/ikuta-shrine/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('ikuta-shrine', 'ja', '生田神社', '神社', '神戸の地名発祥とされる古社。縁結びのご利益で知られ、三宮の中心部にありながら静かな杜に囲まれている。', '7:00〜18:30（季節により変動）', '神戸市中央区下山手通1丁目2-1'),
    ('ikuta-shrine', 'en', 'Ikuta Shrine', 'Shrine', 'An ancient shrine said to be the origin of Kobe''s name, renowned for matchmaking blessings and set within a tranquil grove in central Sannomiya.', '7:00-18:30 (varies by season)', '1-2-1 Shimoyamate-dori, Chuo Ward, Kobe'),
    ('ikuta-shrine', 'zh', '生田神社', '神社', '相传为「神户」地名由来的古老神社，以缔结良缘闻名，虽位于三宫市中心却被静谧树林环绕。', '7:00-18:30（因季节而异）', '神户市中央区下山手通1丁目2-1'),
    ('ikuta-shrine', 'ko', '이쿠타 신사', '신사', '고베라는 지명의 유래로 알려진 유서 깊은 신사로, 인연을 맺어주는 영험함으로 유명하며 산노미야 중심부에 있으면서도 고요한 숲에 둘러싸여 있다.', '7:00~18:30 (계절에 따라 변동)', '고베시 주오구 시모야마테도리 1초메 2-1');

-- 神戸市立博物館
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-city-museum', 'history', 34.6885, 135.1875, 'https://images.kobe-pocket.example.com/spots/kobe-city-museum/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-city-museum', 'ja', '神戸市立博物館', '博物館', '南蛮美術や神戸の開港以来の歴史資料を収蔵する博物館。旧居留地の趣ある建物も見どころ。', '9:30〜17:30（金・土は20:00まで）、月曜定休', '神戸市中央区京町24'),
    ('kobe-city-museum', 'en', 'Kobe City Museum', 'Museum', 'A museum housing Nanban art and historical artifacts tracing Kobe''s history since its port opened, set in a stately former settlement building.', '9:30-17:30 (until 20:00 Fri & Sat), closed Mondays', '24 Kyomachi, Chuo Ward, Kobe'),
    ('kobe-city-museum', 'zh', '神户市立博物馆', '博物馆', '收藏南蛮美术及神户开港以来历史资料的博物馆，坐落于风格独特的旧居留地建筑内。', '9:30-17:30（周五、周六至20:00），周一休馆', '神户市中央区京町24'),
    ('kobe-city-museum', 'ko', '고베시립 박물관', '박물관', '난반 미술품과 고베 개항 이래의 역사 자료를 소장한 박물관으로, 옛 거류지의 운치 있는 건물도 볼거리이다.', '9:30~17:30 (금·토요일은 20:00까지), 월요일 휴관', '고베시 주오구 교마치 24');

-- 神戸須磨シーワールド
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-suma-seaworld', 'nature', 34.6497, 135.1198, 'https://images.kobe-pocket.example.com/spots/kobe-suma-seaworld/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-suma-seaworld', 'ja', '神戸須磨シーワールド', '水族館', '須磨海岸に面した水族館。イルカやシャチのパフォーマンスなど多彩な海の生き物と出会える。', '平日10:00〜18:00、土日祝10:00〜20:00（季節変動あり）', '神戸市須磨区若宮町1丁目3-5'),
    ('kobe-suma-seaworld', 'en', 'Kobe Suma Seaworld', 'Aquarium', 'An aquarium on Suma Beach offering dolphin and orca shows alongside a wide variety of marine life.', 'Weekdays 10:00-18:00, weekends/holidays 10:00-20:00 (seasonal variation)', '1-3-5 Wakamiyacho, Suma Ward, Kobe'),
    ('kobe-suma-seaworld', 'zh', '神户须磨海洋世界', '水族馆', '面向须磨海岸的水族馆，可欣赏海豚、虎鲸表演，邂逅多种多样的海洋生物。', '平日10:00-18:00，周末及节假日10:00-20:00（因季节而异）', '神户市须磨区若宫町1丁目3-5'),
    ('kobe-suma-seaworld', 'ko', '고베 스마 시월드', '수족관', '스마 해안에 면한 수족관으로, 돌고래와 범고래 공연 등 다채로운 해양생물을 만날 수 있다.', '평일 10:00~18:00, 주말·공휴일 10:00~20:00 (계절에 따라 변동)', '고베시 스마구 와카미야초 1초메 3-5');

-- 舞子公園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('maiko-park', 'landmark', 34.6349, 135.0008, 'https://images.kobe-pocket.example.com/spots/maiko-park/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('maiko-park', 'ja', '舞子公園', '展望スポット', '明石海峡大橋を間近に望む公園。橋の下を歩ける「舞子海上プロムナード」からの眺めは圧巻。', '入園自由（舞子海上プロムナードは9:00〜18:00）', '神戸市垂水区東舞子町2051'),
    ('maiko-park', 'en', 'Maiko Park', 'Scenic Spot', 'A park offering close-up views of the Akashi Kaikyo Bridge, with the Maiko Marine Promenade allowing visitors to walk beneath the bridge deck.', 'Open freely (Maiko Marine Promenade 9:00-18:00)', '2051 Higashimaikocho, Tarumi Ward, Kobe'),
    ('maiko-park', 'zh', '舞子公园', '观景点', '可近距离眺望明石海峡大桥的公园，「舞子海上长廊」可漫步于桥下，景色震撼。', '免费开放（舞子海上长廊9:00-18:00）', '神户市垂水区东舞子町2051'),
    ('maiko-park', 'ko', '마이코 공원', '전망 명소', '아카시 해협 대교를 가까이서 볼 수 있는 공원으로, 다리 아래를 걸을 수 있는 ''마이코 해상 프롬나드''에서의 전망이 압권이다.', '자유 입장 (마이코 해상 프롬나드는 9:00~18:00)', '고베시 다루미구 히가시마이코초 2051');

-- 相楽園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('sorakuen', 'history', 34.6912, 135.1839, 'https://images.kobe-pocket.example.com/spots/sorakuen/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('sorakuen', 'ja', '相楽園', '日本庭園', '神戸中心部に残る唯一の日本庭園。旧小寺家厩舎など歴史的建造物とともに四季の景観を楽しめる。', '9:00〜17:00（入園16:30まで）、木曜定休', '神戸市中央区中山手通5丁目3-1'),
    ('sorakuen', 'en', 'Sorakuen Garden', 'Japanese Garden', 'The only Japanese-style garden remaining in central Kobe, featuring historic structures such as the former Kodera family stable alongside seasonal scenery.', '9:00-17:00 (last entry 16:30), closed Thursdays', '5-3-1 Nakayamate-dori, Chuo Ward, Kobe'),
    ('sorakuen', 'zh', '相乐园', '日式庭园', '神户市中心仅存的日式庭园，园内保留旧小寺家马厩等历史建筑，四季景色各具魅力。', '9:00-17:00（入园至16:30），周四休园', '神户市中央区中山手通5丁目3-1'),
    ('sorakuen', 'ko', '소라쿠엔 정원', '일본식 정원', '고베 중심부에 남아 있는 유일한 일본식 정원으로, 옛 고데라가 마구간 등 역사적 건축물과 함께 사계절의 풍경을 즐길 수 있다.', '9:00~17:00 (입장은 16:30까지), 목요일 휴무', '고베시 주오구 나카야마테도리 5초메 3-1');

-- 神戸布引ハーブ園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-nunobiki-herb-garden', 'nature', 34.7025, 135.1962, 'https://images.kobe-pocket.example.com/spots/kobe-nunobiki-herb-garden/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-nunobiki-herb-garden', 'ja', '神戸布引ハーブ園', '植物園', 'ロープウェイで登る日本最大級のハーブ園。標高400mからの神戸の街並みと季節の花々が魅力。', 'ロープウェイ 9:30〜20:15、ハーブ園 10:00〜20:30（季節変動あり）', '神戸市中央区北野町1-4-3'),
    ('kobe-nunobiki-herb-garden', 'en', 'Kobe Nunobiki Herb Garden', 'Botanical Garden', 'Japan''s largest herb garden, accessible by ropeway, offering panoramic views of Kobe from 400m elevation alongside seasonal flowers.', 'Ropeway 9:30-20:15, garden 10:00-20:30 (seasonal variation)', '1-4-3 Kitanocho, Chuo Ward, Kobe'),
    ('kobe-nunobiki-herb-garden', 'zh', '神户布引香草园', '植物园', '乘缆车即可抵达的日本最大级香草园，从海拔400米俯瞰神户街景，四季花卉引人入胜。', '缆车9:30-20:15，香草园10:00-20:30（因季节而异）', '神户市中央区北野町1-4-3'),
    ('kobe-nunobiki-herb-garden', 'ko', '고베 누노비키 허브원', '식물원', '로프웨이를 타고 오르는 일본 최대급 허브원으로, 해발 400m에서 바라보는 고베 시가지와 계절 꽃들이 매력적이다.', '로프웨이 9:30~20:15, 허브원 10:00~20:30 (계절에 따라 변동)', '고베시 주오구 기타노초 1-4-3');
