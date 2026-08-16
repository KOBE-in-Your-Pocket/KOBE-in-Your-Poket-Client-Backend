-- #162: 避難所 type の wire 値を Client 契約に合わせる。
--
-- V7 のコメントも ShelterType の KDoc も「Client のリテラルに一致させる」と書いていたが、
-- 実際の値は災害対策基本法の用語をそのまま slug 化した長い形で、Client の ShelterType
-- （'emergency' | 'designated' | 'both' / domain/evacuation-shelter.ts）と噛み合っていなかった。
-- docs/architecture.md §7.0「ドメインモデルは Client の Mock API スキーマを正として設計する」
-- に従い、Backend 側を Client へ寄せる。
--
--   designated-emergency-evacuation-site -> emergency    （指定緊急避難場所）
--   designated-evacuation-shelter        -> designated   （指定避難所）
--   dual-use                             -> both         （兼用）
--
-- 旧値は V7 の CHECK 制約で固定されているため DROP -> UPDATE -> 再作成の順で行う。
-- 順序を入れ替えると UPDATE が制約に弾かれる。
--
-- このマイグレーションと ShelterType.wireValue の変更は必ず同一リリースで出すこと。
-- ShelterEntity.toDomain は未知の type コードを error() で落とすため、片方だけ先に出ると
-- 一覧 API が 500 になる。

ALTER TABLE shelter DROP CONSTRAINT ck_shelter_type;

UPDATE shelter
SET type = CASE type
    WHEN 'designated-emergency-evacuation-site' THEN 'emergency'
    WHEN 'designated-evacuation-shelter'        THEN 'designated'
    WHEN 'dual-use'                             THEN 'both'
    -- 想定外の値はここで変換せず、直後の CHECK 追加で移行ごと失敗させる（黙って通さない）
    ELSE type
END;

ALTER TABLE shelter ADD CONSTRAINT ck_shelter_type CHECK (type IN (
    'emergency',
    'designated',
    'both'
));
