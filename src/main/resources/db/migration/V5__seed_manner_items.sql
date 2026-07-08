-- feature ④（manner）の初期データ投入（seed）。
-- 要件 #71: 神戸特有（背景説明付き）2 件以上 + 日本全般を含め計 5 件以上、4 言語（ja/en/ko/zh）。
-- Client のモックデータ要件（Client#215 / #231）と件数・内容を整合させる想定。
--
-- 内訳: 神戸特有（scope=local）3 件 + 日本全般（scope=japan）5 件 = 計 8 件。
-- relatedSpotIds は M-2 の ID 参照のみ（spot への FK は張らない）。六甲山のマナーを
-- Tourism の spot `mount-rokko` に関連づける参照を 1 件含める。

-- ── ベース行（manner_item） ─────────────────────────────────────────
INSERT INTO manner_item (id, icon, kind, scope) VALUES
    ('arima-onsen-bathing',      'hot-spring', 'manner', 'local'),
    ('rokko-nature-protection',  'mountain',   'manner', 'local'),
    ('nankinmachi-street-food',  'food',       'manner', 'local'),
    ('no-littering',             'trash',      'rule',   'japan'),
    ('train-quiet',              'train',      'manner', 'japan'),
    ('orderly-queue',            'users',      'manner', 'japan'),
    ('shrine-temple-etiquette',  'torii',      'manner', 'japan'),
    ('no-tipping',               'coin-off',   'manner', 'japan');

-- ── 関連スポット参照（manner_item_spot） ────────────────────────────
INSERT INTO manner_item_spot (manner_item_id, spot_id) VALUES
    ('rokko-nature-protection', 'mount-rokko');

-- ── ローカライズ（manner_item_localization） ────────────────────────
INSERT INTO manner_item_localization (manner_item_id, language, title, description) VALUES
    -- 1. 有馬温泉の入浴マナー（神戸特有）
    ('arima-onsen-bathing', 'ja', '有馬温泉の入浴マナー', '日本三古湯のひとつ有馬温泉では、湯船に入る前にかけ湯で体を流し、タオルや髪を湯につけないようにしましょう。'),
    ('arima-onsen-bathing', 'en', 'Arima Onsen bathing etiquette', 'Arima is one of Japan''s oldest hot springs. Rinse your body before entering the bath, and keep towels and hair out of the water.'),
    ('arima-onsen-bathing', 'ko', '아리마 온천 입욕 매너', '일본에서 가장 오래된 온천 중 하나인 아리마 온천에서는 탕에 들어가기 전에 몸을 헹구고, 수건과 머리카락이 물에 닿지 않도록 하세요.'),
    ('arima-onsen-bathing', 'zh', '有马温泉入浴礼仪', '有马温泉是日本最古老的温泉之一。入浴前请先冲净身体，并不要将毛巾或头发泡入浴池。'),

    -- 2. 六甲山の自然保護（神戸特有）
    ('rokko-nature-protection', 'ja', '六甲山の自然保護', '六甲山は市街地に近い場所に貴重な自然が残る山です。ゴミは必ず持ち帰り、動植物の採取や登山道以外への立ち入りは控えましょう。'),
    ('rokko-nature-protection', 'en', 'Protect Mount Rokko''s nature', 'Mount Rokko preserves rare nature close to the city. Take all your trash home, and refrain from picking plants or leaving the marked trails.'),
    ('rokko-nature-protection', 'ko', '롯코산의 자연 보호', '롯코산은 도심과 가까운 곳에 귀중한 자연이 남아 있는 산입니다. 쓰레기는 반드시 되가져가고, 동식물 채취나 등산로 외 출입은 삼가세요.'),
    ('rokko-nature-protection', 'zh', '保护六甲山的自然', '六甲山保留着邻近市区的珍贵自然环境。请务必将垃圾带走，勿采摘动植物或进入登山道以外的地方。'),

    -- 3. 南京町の食べ歩きマナー（神戸特有）
    ('nankinmachi-street-food', 'ja', '南京町の食べ歩きマナー', '神戸南京町は道幅が狭く混み合います。食べ歩きの際は通行の妨げにならないよう端に寄り、ゴミは各店やゴミ箱へ捨てましょう。'),
    ('nankinmachi-street-food', 'en', 'Eating while walking in Nankinmachi', 'Kobe''s Nankinmachi (Chinatown) is narrow and crowded. Step to the side while eating, and dispose of trash at shops or bins.'),
    ('nankinmachi-street-food', 'ko', '난킨마치 먹거리 매너', '고베 난킨마치(차이나타운)는 길이 좁고 붐빕니다. 걸으며 먹을 때는 통행에 방해가 되지 않도록 가장자리로 비켜서고, 쓰레기는 가게나 쓰레기통에 버리세요.'),
    ('nankinmachi-street-food', 'zh', '南京町边走边吃的礼仪', '神户南京町（中华街）道路狭窄且拥挤。边走边吃时请靠边，避免妨碍通行，并将垃圾丢到店家或垃圾桶。'),

    -- 4. ゴミのポイ捨て禁止（日本全般）
    ('no-littering', 'ja', 'ゴミのポイ捨て禁止', '日本では路上のゴミ箱が少なく、ゴミは持ち帰るのが基本です。ポイ捨ては法律で罰せられることもあります。'),
    ('no-littering', 'en', 'No littering', 'Public bins are scarce in Japan, so please carry your trash with you. Littering can be subject to fines.'),
    ('no-littering', 'ko', '쓰레기 무단 투기 금지', '일본은 거리의 쓰레기통이 적어 쓰레기는 되가져가는 것이 기본입니다. 무단 투기는 법으로 처벌받을 수 있습니다.'),
    ('no-littering', 'zh', '禁止乱扔垃圾', '日本街头的垃圾桶很少，垃圾原则上要自行带走。乱扔垃圾可能会被处以罚款。'),

    -- 5. 電車内は静かに（日本全般）
    ('train-quiet', 'ja', '電車内は静かに', '電車やバスの車内では通話を控え、携帯電話はマナーモードにしましょう。優先座席付近では混雑時に電源を切ります。'),
    ('train-quiet', 'en', 'Keep quiet on trains', 'Avoid phone calls on trains and buses and set your phone to silent. Near priority seats, switch it off when crowded.'),
    ('train-quiet', 'ko', '전철 안에서는 조용히', '전철이나 버스 안에서는 통화를 삼가고 휴대폰은 매너모드로 하세요. 노약자석 부근에서는 혼잡 시 전원을 꺼 주세요.'),
    ('train-quiet', 'zh', '在电车内保持安静', '在电车和巴士内请勿通话，并将手机调至静音。在优先座席附近，拥挤时请关闭手机电源。'),

    -- 6. 列に並んで待つ（日本全般）
    ('orderly-queue', 'ja', '列に並んで待つ', '駅のホームや店舗では、割り込まずに列の最後尾に並びます。電車を待つ際は降りる人を先に通しましょう。'),
    ('orderly-queue', 'en', 'Wait in an orderly line', 'Line up at the back without cutting in, whether on platforms or at shops. Let passengers off the train before boarding.'),
    ('orderly-queue', 'ko', '줄을 서서 기다리기', '역 승강장이나 상점에서는 새치기하지 말고 줄 맨 뒤에 서세요. 전철을 탈 때는 내리는 사람을 먼저 보내 주세요.'),
    ('orderly-queue', 'zh', '排队等候', '在站台或店铺请勿插队，到队伍最后排队。等电车时请先让下车的人通过。'),

    -- 7. 神社・寺院での作法（日本全般）
    ('shrine-temple-etiquette', 'ja', '神社・寺院での作法', '参道の中央は神様の通り道とされるため端を歩き、手水舎で手と口を清めてから参拝しましょう。撮影禁止の場所では従ってください。'),
    ('shrine-temple-etiquette', 'en', 'Shrine and temple etiquette', 'Walk to the side of the path, purify your hands and mouth at the water basin before praying, and obey no-photography signs.'),
    ('shrine-temple-etiquette', 'ko', '신사·사찰에서의 예절', '참배로 중앙은 신의 길로 여겨지므로 가장자리로 걷고, 데미즈야에서 손과 입을 씻은 뒤 참배하세요. 촬영 금지 장소에서는 이를 따르세요.'),
    ('shrine-temple-etiquette', 'zh', '神社与寺院的礼仪', '参道中央被视为神明通行之路，请靠边行走，并在手水舍净手漱口后再参拜。在禁止拍照的场所请遵守规定。'),

    -- 8. チップは不要（日本全般）
    ('no-tipping', 'ja', 'チップは不要', '日本にはチップの習慣がなく、料金には基本的にサービス料が含まれています。無理に渡すと断られることがあります。'),
    ('no-tipping', 'en', 'No tipping needed', 'Tipping is not customary in Japan, and service is generally included in the price. Staff may politely decline tips.'),
    ('no-tipping', 'ko', '팁은 필요 없습니다', '일본에는 팁 문화가 없으며 요금에 기본적으로 서비스 요금이 포함되어 있습니다. 억지로 건네면 정중히 거절당할 수 있습니다.'),
    ('no-tipping', 'zh', '无需支付小费', '日本没有付小费的习惯，费用中通常已包含服务费。勉强给小费有时会被婉拒。');
