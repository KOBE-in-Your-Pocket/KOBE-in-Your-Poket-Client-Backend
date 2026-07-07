-- #62: 実スポット seed データ（神戸の実在観光地16件、ja/en/zh/ko 4言語ローカライズ）を投入する。
-- モックの `mock-spots`（Client 側）を置き換える実データの初回セットで、Client の
-- mock-spots 廃止（KOBE-in-Your-Pocket/KOBE-in-Your-Poket-Client#305）に先立ち整備する。
--
-- image_url は要件定義 O-2（Client `buildSpotImageUrl` 引退）に合わせて完全 URL 形式で格納する。
-- ただし実画像（権利処理済み）の調達・アップロードは運営側の別タスクのため、本マイグレーションでは
-- `https://images.kobe-pocket.example.com/spots/{id}/main.jpg` のプレースホルダー URL を暫定で格納する。
-- 実 CDN/S3 URL が確定し次第、UPDATE で差し替える想定。
--
-- rating_value は feature ②（レビュー）由来の派生値のため、V1 の方針どおり NULL のまま登録する。

-- 北野異人館街
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kitano-ijinkan', 'landmark', 34.6996, 135.1958, 'https://images.kobe-pocket.example.com/spots/kitano-ijinkan/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kitano-ijinkan', 'ja', '北野異人館街', '定番スポット', '明治から大正期に建てられた異人館が立ち並ぶ異国情緒あふれる散策エリア。「うろこの家」など人気の館が点在する。', '館により異なる（目安 9:00〜18:00、無休）', '兵庫県神戸市中央区北野町3丁目'),
    ('kitano-ijinkan', 'en', 'Kitano Ijinkan (Foreigners'' District)', 'Landmark', 'A charming hillside district lined with Meiji- and Taisho-era Western-style houses, including the popular House of Scales (Uroko-no-Ie).', 'Varies by house (approx. 9:00-18:00, open daily)', '3-chome, Kitano-cho, Chuo-ku, Kobe, Hyogo'),
    ('kitano-ijinkan', 'zh', '北野异人馆街', '地标景点', '明治至大正时期建造的西式馆舍林立的异国风情街区，「鳞之家」等人气馆舍点缀其中。', '因馆而异（约9:00-18:00，全年无休）', '兵库县神户市中央区北野町3丁目'),
    ('kitano-ijinkan', 'ko', '기타노 이진칸 거리', '랜드마크', '메이지·다이쇼 시대에 지어진 서양식 저택이 늘어선 이국적인 산책 명소로, ''비늘의 집'' 등 인기 저택이 자리한다.', '저택마다 다름 (대략 9:00~18:00, 연중무휴)', '효고현 고베시 주오구 기타노초 3초메');

-- メリケンパーク
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('meriken-park', 'landmark', 34.6853, 135.1885, 'https://images.kobe-pocket.example.com/spots/meriken-park/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('meriken-park', 'ja', 'メリケンパーク', '定番スポット', '神戸港に面したウォーターフロント公園。BE KOBEモニュメントや神戸海洋博物館があり、港町神戸を象徴する景観が広がる。', '入園自由（24時間開放）', '兵庫県神戸市中央区波止場町2'),
    ('meriken-park', 'en', 'Meriken Park', 'Landmark', 'A waterfront park facing Kobe Port, home to the BE KOBE monument and the Kobe Maritime Museum, symbolizing the city''s port heritage.', 'Open 24 hours, free admission', '2 Hatobacho, Chuo-ku, Kobe, Hyogo'),
    ('meriken-park', 'zh', '美利坚公园', '地标景点', '面向神户港的滨海公园，园内有BE KOBE纪念碑和神户海洋博物馆，展现港口城市神户的象征性景观。', '全天开放，免费入园', '兵库县神户市中央区波止场町2'),
    ('meriken-park', 'ko', '메리켄 파크', '랜드마크', '고베항에 면한 워터프런트 공원으로, BE KOBE 모뉴먼트와 고베 해양박물관이 있어 항구 도시 고베를 상징하는 경관을 볼 수 있다.', '24시간 개방, 입장 무료', '효고현 고베시 주오구 하토바초 2');

-- 神戸ポートタワー
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-port-tower', 'landmark', 34.6841, 135.1876, 'https://images.kobe-pocket.example.com/spots/kobe-port-tower/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-port-tower', 'ja', '神戸ポートタワー', '定番スポット', '鼓の形をした赤い鉄塔展望台。神戸港とメリケンパークを一望できる神戸のシンボル的存在。', '9:00〜23:00（最終入場22:30）、年中無休', '兵庫県神戸市中央区波止場町5-5'),
    ('kobe-port-tower', 'en', 'Kobe Port Tower', 'Landmark', 'A red steel tower shaped like a traditional tsuzumi drum, offering panoramic views of Kobe Port and Meriken Park. A defining symbol of the city.', '9:00-23:00 (last entry 22:30), open daily', '5-5 Hatobacho, Chuo-ku, Kobe, Hyogo'),
    ('kobe-port-tower', 'zh', '神户港塔', '地标景点', '形似日本传统鼓「鼓」的红色铁塔展望台，可俯瞰神户港与美利坚公园，是神户的标志性建筑。', '9:00-23:00（最终入场22:30），全年无休', '兵库县神户市中央区波止场町5-5'),
    ('kobe-port-tower', 'ko', '고베 포트 타워', '랜드마크', '일본 전통 북 ''츠즈미'' 모양을 한 붉은 철탑 전망대로, 고베항과 메리켄 파크를 한눈에 볼 수 있는 고베의 상징이다.', '9:00~23:00 (최종 입장 22:30), 연중무휴', '효고현 고베시 주오구 하토바초 5-5');

-- 南京町
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('nankinmachi', 'gourmet', 34.6890, 135.1900, 'https://images.kobe-pocket.example.com/spots/nankinmachi/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('nankinmachi', 'ja', '南京町', 'グルメ', '横浜・長崎と並ぶ日本三大中華街のひとつ。食べ歩きグルメや雑貨店が並ぶにぎやかなエリア。', '店舗により異なる（目安 10:00〜20:00）', '兵庫県神戸市中央区栄町通・元町通1丁目'),
    ('nankinmachi', 'en', 'Nankinmachi (Kobe Chinatown)', 'Gourmet', 'One of Japan''s three major Chinatowns, alongside Yokohama and Nagasaki. A lively area packed with street-food stalls and specialty shops.', 'Varies by shop (approx. 10:00-20:00)', 'Sakaemachi-dori / Motomachi-dori 1-chome, Chuo-ku, Kobe, Hyogo'),
    ('nankinmachi', 'zh', '南京町（神户中华街）', '美食', '与横滨、长崎齐名的日本三大中华街之一，聚集了众多小吃摊和杂货店，热闹非凡。', '因店而异（约10:00-20:00）', '兵库县神户市中央区荣町通・元町通1丁目'),
    ('nankinmachi', 'ko', '난킨마치 (고베 차이나타운)', '미식', '요코하마, 나가사키와 함께 일본 3대 차이나타운으로 꼽히며, 먹거리 노점과 잡화점이 늘어선 활기찬 거리이다.', '점포마다 다름 (대략 10:00~20:00)', '효고현 고베시 주오구 사카에마치도리·모토마치도리 1초메');

-- 神戸ハーバーランド
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-harborland', 'landmark', 34.6819, 135.1808, 'https://images.kobe-pocket.example.com/spots/kobe-harborland/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-harborland', 'ja', '神戸ハーバーランド', '定番スポット', '神戸港に面した複合商業エリア。umieやモザイクなどの商業施設が集まり、夜景スポットとしても人気。', '施設により異なる（目安 10:00〜20:00）', '兵庫県神戸市中央区東川崎町1丁目'),
    ('kobe-harborland', 'en', 'Kobe Harborland', 'Landmark', 'A waterfront shopping and entertainment district on Kobe Port, home to umie and Mosaic, and a popular spot for night views.', 'Varies by facility (approx. 10:00-20:00)', '1-chome, Higashikawasakicho, Chuo-ku, Kobe, Hyogo'),
    ('kobe-harborland', 'zh', '神户海港乐园', '地标景点', '面向神户港的综合商业区，umie、Mosaic等商业设施云集，也是热门的夜景观赏地。', '因设施而异（约10:00-20:00）', '兵库县神户市中央区东川崎町1丁目'),
    ('kobe-harborland', 'ko', '고베 하버랜드', '랜드마크', '고베항에 면한 복합 상업지구로, umie와 모자이크 등의 상업시설이 모여 있으며 야경 명소로도 인기가 높다.', '시설마다 다름 (대략 10:00~20:00)', '효고현 고베시 주오구 히가시카와사키초 1초메');

-- 有馬温泉 金の湯
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('arima-onsen-kinnoyu', 'onsen', 34.7975, 135.2481, 'https://images.kobe-pocket.example.com/spots/arima-onsen-kinnoyu/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('arima-onsen-kinnoyu', 'ja', '有馬温泉 金の湯', '温泉', '日本三古湯のひとつ有馬温泉を代表する外湯。赤茶色の「金泉」を気軽に日帰りで楽しめる。', '8:00〜22:00（最終受付21:30）、第2・第4火曜定休', '兵庫県神戸市北区有馬町833'),
    ('arima-onsen-kinnoyu', 'en', 'Arima Onsen Kin-no-yu', 'Hot Spring', 'A public bathhouse in Arima Onsen, one of Japan''s three oldest hot spring resorts, known for its iron-rich, reddish-brown golden waters.', '8:00-22:00 (last entry 21:30), closed 2nd & 4th Tuesdays', '833 Arima-cho, Kita-ku, Kobe, Hyogo'),
    ('arima-onsen-kinnoyu', 'zh', '有马温泉 金之汤', '温泉', '日本三大古汤之一有马温泉的代表性公共浴场，可轻松体验红褐色的「金泉」。', '8:00-22:00（最终入场21:30），逢第2、第4个周二休息', '兵库县神户市北区有马町833'),
    ('arima-onsen-kinnoyu', 'ko', '아리마 온천 킨노유', '온천', '일본 3대 고대 온천 중 하나인 아리마 온천을 대표하는 외탕으로, 붉은빛을 띠는 ''긴센(금천)''을 당일치기로 즐길 수 있다.', '8:00~22:00 (최종 접수 21:30), 둘째·넷째 화요일 휴무', '효고현 고베시 기타구 아리마초 833');

-- 六甲ガーデンテラス
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('rokko-garden-terrace', 'nature', 34.7307, 135.2436, 'https://images.kobe-pocket.example.com/spots/rokko-garden-terrace/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('rokko-garden-terrace', 'ja', '六甲ガーデンテラス', '自然', '六甲山頂に広がる展望施設。神戸・大阪の街並みと大阪湾を一望でき、「六甲枝垂れ」など夜景スポットとしても名高い。', '9:30〜21:00（季節・施設により変動）', '兵庫県神戸市灘区六甲山町五介山1877-9'),
    ('rokko-garden-terrace', 'en', 'Rokko Garden Terrace', 'Nature', 'An observation complex atop Mt. Rokko offering sweeping views of Kobe, Osaka, and Osaka Bay, famed for its night views including the Rokko Shidare observatory.', '9:30-21:00 (varies by season and facility)', '1877-9 Gokaisan, Rokkosanmachi, Nada-ku, Kobe, Hyogo'),
    ('rokko-garden-terrace', 'zh', '六甲花园露台', '自然风光', '位于六甲山顶的展望设施，可一览神户、大阪的街景与大阪湾，也因「六甲枝垂」等夜景景点而闻名。', '9:30-21:00（因季节、设施而异）', '兵库县神户市滩区六甲山町五介山1877-9'),
    ('rokko-garden-terrace', 'ko', '롯코 가든 테라스', '자연', '롯코산 정상에 펼쳐진 전망 시설로, 고베·오사카 시가지와 오사카만을 한눈에 볼 수 있으며 ''롯코 시다레'' 등 야경 명소로도 유명하다.', '9:30~21:00 (계절·시설에 따라 변동)', '효고현 고베시 나다구 롯코산초 고카이산 1877-9');

-- 布引の滝
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('nunobiki-falls', 'nature', 34.7017, 135.1949, 'https://images.kobe-pocket.example.com/spots/nunobiki-falls/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('nunobiki-falls', 'ja', '布引の滝', '自然', '「日本三大神滝」に数えられる名瀑。雄滝・雌滝など4つの滝の総称で、新神戸駅から徒歩圏内で楽しめる。', '見学自由（散策路は日中の利用を推奨）', '兵庫県神戸市中央区葺合町布引山'),
    ('nunobiki-falls', 'en', 'Nunobiki Falls', 'Nature', 'One of Japan''s three sacred waterfalls, comprising four falls including Otaki and Mentaki, reachable on foot from Shin-Kobe Station.', 'Freely accessible (daytime visits recommended)', 'Nunobikiyama, Fukiai-cho, Chuo-ku, Kobe, Hyogo'),
    ('nunobiki-falls', 'zh', '布引瀑布', '自然风光', '被誉为「日本三大神瀑」之一，由雄泷、雌泷等四条瀑布组成，从新神户站步行即可抵达。', '自由参观（建议白天前往）', '兵库县神户市中央区苇合町布引山'),
    ('nunobiki-falls', 'ko', '누노비키 폭포', '자연', '''일본 3대 신성한 폭포'' 중 하나로 꼽히며 오타키·메타키 등 4개의 폭포로 이루어져 있으며, 신고베역에서 도보로 갈 수 있다.', '자유 관람 (주간 방문 권장)', '효고현 고베시 주오구 후키아이초 누노비키야마');

-- 神戸市立王子動物園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('oji-zoo', 'nature', 34.7096, 135.2126, 'https://images.kobe-pocket.example.com/spots/oji-zoo/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('oji-zoo', 'ja', '神戸市立王子動物園', '自然', '約120種700点の動物と出会える動物園。桜の名所としても知られ、遊園地も併設する。', '9:00〜17:00（11〜2月は16:30まで）、水曜定休', '兵庫県神戸市灘区王子町3-1'),
    ('oji-zoo', 'en', 'Kobe Municipal Oji Zoo', 'Nature', 'Home to around 120 species and 700 animals, this zoo is also a popular cherry-blossom spot and includes an amusement park.', '9:00-17:00 (until 16:30 Nov-Feb), closed Wednesdays', '3-1 Ojicho, Nada-ku, Kobe, Hyogo'),
    ('oji-zoo', 'zh', '神户市立王子动物园', '自然风光', '可邂逅约120种700只动物的动物园，也是著名的赏樱胜地，园内还设有游乐场。', '9:00-17:00（11月至2月至16:30），周三休园', '兵库县神户市滩区王子町3-1'),
    ('oji-zoo', 'ko', '고베시립 오지 동물원', '자연', '약 120종 700여 마리의 동물을 만날 수 있는 동물원으로, 벚꽃 명소로도 알려져 있으며 놀이공원도 함께 있다.', '9:00~17:00 (11~2월은 16:30까지), 수요일 휴무', '효고현 고베시 나다구 오지초 3-1');

-- 摩耶山掬星台
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('mayasan-kikuseidai', 'nature', 34.7204, 135.2278, 'https://images.kobe-pocket.example.com/spots/mayasan-kikuseidai/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('mayasan-kikuseidai', 'ja', '摩耶山掬星台', '自然', '標高702mから大阪湾までを見渡す展望台。日本三大夜景のひとつに数えられる絶景が広がる。', '見学自由（ロープウェイ運行時間に準ずる）', '兵庫県神戸市灘区摩耶山町2-2'),
    ('mayasan-kikuseidai', 'en', 'Mt. Maya Kikuseidai', 'Nature', 'An observation deck at 702m elevation overlooking Osaka Bay, celebrated as one of Japan''s three great night views.', 'Freely accessible (subject to ropeway operating hours)', '2-2 Mayasanmachi, Nada-ku, Kobe, Hyogo'),
    ('mayasan-kikuseidai', 'zh', '摩耶山掬星台', '自然风光', '海拔702米的展望台，可远眺大阪湾，是日本三大夜景之一的绝美景观。', '自由参观（依缆车运行时间而定）', '兵库县神户市滩区摩耶山町2-2'),
    ('mayasan-kikuseidai', 'ko', '마야산 기쿠세이다이', '자연', '해발 702m에서 오사카만까지 내려다보이는 전망대로, 일본 3대 야경 중 하나로 꼽히는 절경을 자랑한다.', '자유 관람 (로프웨이 운행 시간에 준함)', '효고현 고베시 나다구 마야산초 2-2');

-- 生田神社
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('ikuta-shrine', 'history', 34.6939, 135.1926, 'https://images.kobe-pocket.example.com/spots/ikuta-shrine/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('ikuta-shrine', 'ja', '生田神社', '歴史・文化', '神戸の地名発祥とされる古社。縁結びのご利益で知られ、三宮の中心部にありながら静かな杜に囲まれている。', '7:00〜18:30（季節により変動）', '兵庫県神戸市中央区下山手通1丁目2-1'),
    ('ikuta-shrine', 'en', 'Ikuta Shrine', 'History & Culture', 'An ancient shrine said to be the origin of Kobe''s name, renowned for matchmaking blessings and set within a tranquil grove in central Sannomiya.', '7:00-18:30 (varies by season)', '1-2-1 Shimoyamate-dori, Chuo-ku, Kobe, Hyogo'),
    ('ikuta-shrine', 'zh', '生田神社', '历史文化', '相传为「神户」地名由来的古老神社，以缔结良缘闻名，虽位于三宫市中心却被静谧树林环绕。', '7:00-18:30（因季节而异）', '兵库县神户市中央区下山手通1丁目2-1'),
    ('ikuta-shrine', 'ko', '이쿠타 신사', '역사·문화', '고베라는 지명의 유래로 알려진 유서 깊은 신사로, 인연을 맺어주는 영험함으로 유명하며 산노미야 중심부에 있으면서도 고요한 숲에 둘러싸여 있다.', '7:00~18:30 (계절에 따라 변동)', '효고현 고베시 주오구 시모야마테도리 1초메 2-1');

-- 神戸市立博物館
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-city-museum', 'history', 34.6885, 135.1875, 'https://images.kobe-pocket.example.com/spots/kobe-city-museum/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-city-museum', 'ja', '神戸市立博物館', '歴史・文化', '南蛮美術や神戸の開港以来の歴史資料を収蔵する博物館。旧居留地の趣ある建物も見どころ。', '9:30〜17:30（金・土は20:00まで）、月曜定休', '兵庫県神戸市中央区京町24'),
    ('kobe-city-museum', 'en', 'Kobe City Museum', 'History & Culture', 'A museum housing Nanban art and historical artifacts tracing Kobe''s history since its port opened, set in a stately former settlement building.', '9:30-17:30 (until 20:00 Fri & Sat), closed Mondays', '24 Kyomachi, Chuo-ku, Kobe, Hyogo'),
    ('kobe-city-museum', 'zh', '神户市立博物馆', '历史文化', '收藏南蛮美术及神户开港以来历史资料的博物馆，坐落于风格独特的旧居留地建筑内。', '9:30-17:30（周五、周六至20:00），周一休馆', '兵库县神户市中央区京町24'),
    ('kobe-city-museum', 'ko', '고베시립 박물관', '역사·문화', '난반 미술품과 고베 개항 이래의 역사 자료를 소장한 박물관으로, 옛 거류지의 운치 있는 건물도 볼거리이다.', '9:30~17:30 (금·토요일은 20:00까지), 월요일 휴관', '효고현 고베시 주오구 교마치 24');

-- 神戸須磨シーワールド
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-suma-seaworld', 'nature', 34.6497, 135.1198, 'https://images.kobe-pocket.example.com/spots/kobe-suma-seaworld/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-suma-seaworld', 'ja', '神戸須磨シーワールド', '自然', '須磨海岸に面した水族館。イルカやシャチのパフォーマンスなど多彩な海の生き物と出会える。', '平日10:00〜18:00、土日祝10:00〜20:00（季節変動あり）', '兵庫県神戸市須磨区若宮町1丁目3-5'),
    ('kobe-suma-seaworld', 'en', 'Kobe Suma Seaworld', 'Nature', 'An aquarium on Suma Beach offering dolphin and orca shows alongside a wide variety of marine life.', 'Weekdays 10:00-18:00, weekends/holidays 10:00-20:00 (seasonal variation)', '1-3-5 Wakamiyacho, Suma-ku, Kobe, Hyogo'),
    ('kobe-suma-seaworld', 'zh', '神户须磨海洋世界', '自然风光', '面向须磨海岸的水族馆，可欣赏海豚、虎鲸表演，邂逅多种多样的海洋生物。', '平日10:00-18:00，周末及节假日10:00-20:00（因季节而异）', '兵库县神户市须磨区若宫町1丁目3-5'),
    ('kobe-suma-seaworld', 'ko', '고베 스마 시월드', '자연', '스마 해안에 면한 수족관으로, 돌고래와 범고래 공연 등 다채로운 해양생물을 만날 수 있다.', '평일 10:00~18:00, 주말·공휴일 10:00~20:00 (계절에 따라 변동)', '효고현 고베시 스마구 와카미야초 1초메 3-5');

-- 舞子公園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('maiko-park', 'landmark', 34.6349, 135.0008, 'https://images.kobe-pocket.example.com/spots/maiko-park/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('maiko-park', 'ja', '舞子公園', '定番スポット', '明石海峡大橋を間近に望む公園。橋の下を歩ける「舞子海上プロムナード」からの眺めは圧巻。', '入園自由（舞子海上プロムナードは9:00〜18:00）', '兵庫県神戸市垂水区東舞子町2051'),
    ('maiko-park', 'en', 'Maiko Park', 'Landmark', 'A park offering close-up views of the Akashi Kaikyo Bridge, with the Maiko Marine Promenade allowing visitors to walk beneath the bridge deck.', 'Open freely (Maiko Marine Promenade 9:00-18:00)', '2051 Higashimaikocho, Tarumi-ku, Kobe, Hyogo'),
    ('maiko-park', 'zh', '舞子公园', '地标景点', '可近距离眺望明石海峡大桥的公园，「舞子海上长廊」可漫步于桥下，景色震撼。', '免费开放（舞子海上长廊9:00-18:00）', '兵库县神户市垂水区东舞子町2051'),
    ('maiko-park', 'ko', '마이코 공원', '랜드마크', '아카시 해협 대교를 가까이서 볼 수 있는 공원으로, 다리 아래를 걸을 수 있는 ''마이코 해상 프롬나드''에서의 전망이 압권이다.', '자유 입장 (마이코 해상 프롬나드는 9:00~18:00)', '효고현 고베시 다루미구 히가시마이코초 2051');

-- 相楽園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('sorakuen', 'history', 34.6912, 135.1839, 'https://images.kobe-pocket.example.com/spots/sorakuen/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('sorakuen', 'ja', '相楽園', '歴史・文化', '神戸中心部に残る唯一の日本庭園。旧小寺家厩舎など歴史的建造物とともに四季の景観を楽しめる。', '9:00〜17:00（入園16:30まで）、木曜定休', '兵庫県神戸市中央区中山手通5丁目3-1'),
    ('sorakuen', 'en', 'Sorakuen Garden', 'History & Culture', 'The only Japanese-style garden remaining in central Kobe, featuring historic structures such as the former Kodera family stable alongside seasonal scenery.', '9:00-17:00 (last entry 16:30), closed Thursdays', '5-3-1 Nakayamate-dori, Chuo-ku, Kobe, Hyogo'),
    ('sorakuen', 'zh', '相乐园', '历史文化', '神户市中心仅存的日式庭园，园内保留旧小寺家马厩等历史建筑，四季景色各具魅力。', '9:00-17:00（入园至16:30），周四休园', '兵库县神户市中央区中山手通5丁目3-1'),
    ('sorakuen', 'ko', '소라쿠엔 정원', '역사·문화', '고베 중심부에 남아 있는 유일한 일본식 정원으로, 옛 고데라가 마구간 등 역사적 건축물과 함께 사계절의 풍경을 즐길 수 있다.', '9:00~17:00 (입장은 16:30까지), 목요일 휴무', '효고현 고베시 주오구 나카야마테도리 5초메 3-1');

-- 神戸布引ハーブ園
INSERT INTO spot (id, genre, latitude, longitude, image_url) VALUES
    ('kobe-nunobiki-herb-garden', 'nature', 34.7025, 135.1962, 'https://images.kobe-pocket.example.com/spots/kobe-nunobiki-herb-garden/main.jpg');
INSERT INTO spot_localization (spot_id, language, name, category_label, description, business_hours, address) VALUES
    ('kobe-nunobiki-herb-garden', 'ja', '神戸布引ハーブ園', '自然', 'ロープウェイで登る日本最大級のハーブ園。標高400mからの神戸の街並みと季節の花々が魅力。', 'ロープウェイ 9:30〜20:15、ハーブ園 10:00〜20:30（季節変動あり）', '兵庫県神戸市中央区北野町1-4-3'),
    ('kobe-nunobiki-herb-garden', 'en', 'Kobe Nunobiki Herb Garden', 'Nature', 'Japan''s largest herb garden, accessible by ropeway, offering panoramic views of Kobe from 400m elevation alongside seasonal flowers.', 'Ropeway 9:30-20:15, garden 10:00-20:30 (seasonal variation)', '1-4-3 Kitanocho, Chuo-ku, Kobe, Hyogo'),
    ('kobe-nunobiki-herb-garden', 'zh', '神户布引香草园', '自然风光', '乘缆车即可抵达的日本最大级香草园，从海拔400米俯瞰神户街景，四季花卉引人入胜。', '缆车9:30-20:15，香草园10:00-20:30（因季节而异）', '兵库县神户市中央区北野町1-4-3'),
    ('kobe-nunobiki-herb-garden', 'ko', '고베 누노비키 허브원', '자연', '로프웨이를 타고 오르는 일본 최대급 허브원으로, 해발 400m에서 바라보는 고베 시가지와 계절 꽃들이 매력적이다.', '로프웨이 9:30~20:15, 허브원 10:00~20:30 (계절에 따라 변동)', '효고현 고베시 주오구 기타노초 1-4-3');
