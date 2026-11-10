# 天気（Yahoo! 気象情報API）

天気マーク（Client のマップ上部）のために、backend が [Yahoo! 気象情報API](https://developer.yahoo.co.jp/webapi/map/openlocalplatform/v1/weather.html) から 15 分ごとに天気を取得して保存し、Client に返す（PBI #226）。  
Client は Yahoo! を直接呼ばない。Client ID は backend だけが持つ。

## API の概要

- 取れるのは **降水強度（mm/h）だけ**。今の観測値と、60 分先までの予測値（10 分ごと）
- 気温や晴れ・曇りは取れない
- 上限は 1 アプリにつき 24 時間で 50,000 回。15 分ごとなら 1 日 96 回。1 回で最大 10 地点
- 認証は Client ID（`appid` パラメータ）だけ

## Client ID の取得

1. Yahoo! JAPAN ID でログインし、[アプリケーションの管理](https://e.developer.yahoo.co.jp/dashboard/) を開く
2. 「新しいアプリケーションを開発」からアプリケーションを登録する
   - アプリケーションの種類: **サーバーサイド**（backend から呼ぶため）
   - 利用する API に気象情報API（YOLP）が含まれていることを確認する
3. 発行された **Client ID** を控える（Client シークレットは使わない）

## 設定する環境変数

| 変数 | 説明 |
| --- | --- |
| `YAHOO_CLIENT_ID` | 上で取得した Client ID。**未設定でも起動はする**が、天気の取得を行わない |

`application.yml` の `yahoo.weather.client-id` に入り、`YahooWeatherProperties` で読む。  
Client ID はログに出さない（`toString` で伏せている）。

### ローカル

`.env` に書く（`.env.example` を参照）。`make dev` / `make up` で `compose.yaml` からコンテナへ渡る。

```bash
YAHOO_CLIENT_ID=取得した Client ID
```

### EC2（本番）

`/opt/kobe-backend/app.env` に 1 行追加し、再デプロイ（または `docker compose up -d`）で反映する。  
Secrets Manager モードの場合はシークレット（既定 `kobe/backend/prod`）にキーを追加する。  
手順の全体は [`infrastructure/ec2-app-env.md`](./infrastructure/ec2-app-env.md) を参照。

```bash
YAHOO_CLIENT_ID=取得した Client ID
```

## クレジット表示・利用条件

Yahoo! のクレジット表示（https://developer.yahoo.co.jp/attribution/ ）と商用利用の条件は #234 で確認する。
