-- #153: 既存スポットが使っている 5 ジャンルをマスタへ投入する。
--
-- spot.genre には既に landmark / nature / history / gourmet / onsen が入っている。
-- マスタが空のままだと、既存スポットのジャンルがマスタに存在しない状態になり、
-- V15 の外部キー制約も張れない。
--
-- 日本語ラベルは ADMIN の GENRE_LABELS、他言語は spot_localization.category_label の
-- 実データ（例: onsen → Hot Spring / 온천）に合わせた。
-- display_order は Client のフィルタで想定している並び（名所→自然→歴史→グルメ→温泉）。

INSERT INTO genre (code, display_order) VALUES
    ('landmark', 1),
    ('nature',   2),
    ('history',  3),
    ('gourmet',  4),
    ('onsen',    5);

INSERT INTO genre_localization (genre_code, language, label) VALUES
    ('landmark', 'ja', '名所'),
    ('landmark', 'en', 'Landmark'),
    ('landmark', 'ko', '명소'),
    ('landmark', 'zh', '名胜'),

    ('nature', 'ja', '自然'),
    ('nature', 'en', 'Nature'),
    ('nature', 'ko', '자연'),
    ('nature', 'zh', '自然'),

    ('history', 'ja', '歴史'),
    ('history', 'en', 'History'),
    ('history', 'ko', '역사'),
    ('history', 'zh', '历史'),

    ('gourmet', 'ja', 'グルメ'),
    ('gourmet', 'en', 'Gourmet'),
    ('gourmet', 'ko', '미식'),
    ('gourmet', 'zh', '美食'),

    ('onsen', 'ja', '温泉'),
    ('onsen', 'en', 'Hot Spring'),
    ('onsen', 'ko', '온천'),
    ('onsen', 'zh', '温泉');
