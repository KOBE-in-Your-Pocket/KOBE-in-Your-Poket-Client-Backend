-- 避難所を神戸市オープンデータの形に合わせる（#180）。
--
-- データ源:
--   神戸市オープンデータカタログ / 指定緊急避難場所・指定避難所（423 件）
--   resource_id 74d6b85b-17a6-4ba8-b7f6-af3ea15f9283
--
-- 従来のシード（11 件）は出所不明のダミーで、画像 URL も実在しないドメインを
-- 指していた。実データへ差し替えるのに伴い、オープンデータが持たない項目
-- （施設種別・画像・収容人数・バリアフリー・外部リンク）を落とし、代わりに
-- 持っている項目（災害種別ごとの適否・屋内屋外・ペット同行・電話番号・備考）を入れる。
-- 持っていない情報を空で返すより、キーごと無い方が Client も迷わない。
--
-- 全行を入れ替えるため、先に DELETE してから NOT NULL 列を追加する
-- （shelter_localization は ON DELETE CASCADE で連動して消える）。
-- 行の投入は V19（生成物）で行う。

DELETE FROM shelter;

ALTER TABLE shelter
    DROP COLUMN facility_category,
    DROP COLUMN image_url,
    DROP COLUMN capacity,
    DROP COLUMN accessible,
    DROP COLUMN external_url;

-- 屋内 or 屋外。オープンデータの「分類」に対応する。
-- 屋外の緊急避難場所は大火事のときに使う広場・公園で、屋内とは性質が違う。
ALTER TABLE shelter ADD COLUMN siting VARCHAR(16) NOT NULL;
ALTER TABLE shelter ADD CONSTRAINT ck_shelter_siting CHECK (siting IN ('indoor', 'outdoor'));

-- 災害種別ごとの適否。オープンデータは ○ / △ / × / － / 空 の 5 値で、
-- △（条件付き）を ○ に丸めると「使えない避難所を使える」と表示してしまう。
-- 空と － はどちらも「その災害では対象外」の意味なので not-applicable に寄せる。
--   suitable        ○ 利用できる
--   conditional     △ 条件付き（備考を読む必要がある）
--   unsuitable      × 利用できない
--   not-applicable  － / 空 対象外
ALTER TABLE shelter
    ADD COLUMN suitability_landslide  VARCHAR(16) NOT NULL,
    ADD COLUMN suitability_flood      VARCHAR(16) NOT NULL,
    ADD COLUMN suitability_tsunami    VARCHAR(16) NOT NULL,
    ADD COLUMN suitability_large_fire VARCHAR(16) NOT NULL;

ALTER TABLE shelter
    ADD CONSTRAINT ck_shelter_suitability_landslide
        CHECK (suitability_landslide IN ('suitable', 'conditional', 'unsuitable', 'not-applicable')),
    ADD CONSTRAINT ck_shelter_suitability_flood
        CHECK (suitability_flood IN ('suitable', 'conditional', 'unsuitable', 'not-applicable')),
    ADD CONSTRAINT ck_shelter_suitability_tsunami
        CHECK (suitability_tsunami IN ('suitable', 'conditional', 'unsuitable', 'not-applicable')),
    ADD CONSTRAINT ck_shelter_suitability_large_fire
        CHECK (suitability_large_fire IN ('suitable', 'conditional', 'unsuitable', 'not-applicable'));

-- ペット同行避難。「調整中」が 38 件あり、可否の 2 値に潰せない。
ALTER TABLE shelter ADD COLUMN pet_acceptance VARCHAR(24) NOT NULL;
ALTER TABLE shelter ADD CONSTRAINT ck_shelter_pet_acceptance
    CHECK (pet_acceptance IN ('accepted', 'not-accepted', 'under-consideration'));

-- 電話番号。屋外の 88 件は元データが空。
ALTER TABLE shelter ADD COLUMN phone_number VARCHAR(32);

-- 備考は言語別。「《土砂災害時》正門が土砂災害警戒区域内にあるので注意、早めに避難」
-- のように避難の判断そのものに関わる文があるため、翻訳して出す必要がある。
ALTER TABLE shelter_localization ADD COLUMN note TEXT;
