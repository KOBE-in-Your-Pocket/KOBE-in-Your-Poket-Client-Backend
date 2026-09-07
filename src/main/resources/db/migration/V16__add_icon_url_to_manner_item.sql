-- 運営がマナー項目のアイコンを画像で差し替えられるようにする（ADMIN の管理画面）。
--
-- これまで icon はアイコン識別キー（要件定義 M-3）で、Client が同梱アセットへ解決していた。
-- キーの追加には Client のリリースが要り、運営だけではアイコンを増やせない。アップロード
-- した画像の URL を持てるようにして、運営側で完結させる。
--
-- icon は残す。既存 8 件はキーしか持たず、Client も当面はキー解決の経路を使うため、
-- 置き換えると移行が必要になる。icon_url があればそちらを優先し、無ければ従来どおり
-- icon のキーで解決する、という二段構えにする。
ALTER TABLE manner_item ADD COLUMN icon_url TEXT;

-- 新規項目は画像だけで登録されうるため、キーの NOT NULL を外す。
ALTER TABLE manner_item ALTER COLUMN icon DROP NOT NULL;

-- どちらも無い項目はアプリでアイコンを描けない。DB でも防ぐ。
ALTER TABLE manner_item
    ADD CONSTRAINT ck_manner_item_icon_present
    CHECK (icon IS NOT NULL OR icon_url IS NOT NULL);
