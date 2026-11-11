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

## 取得バッチ（#232）

`WeatherRefreshScheduler` が、起動 30 秒後に 1 回目、以降 15 分ごとに天気を取得して保存する（`RefreshWeatherService`）。

- 対象は神戸の中心 1 地点（`WeatherPoint.KOBE_CENTER`。神戸市役所・三宮付近）
- 保存先は `weather_readings`。地点ごとに最新の取得分だけを残す（#231）
- 取得に失敗したとき（API エラー・タイムアウト・想定外の応答）は保存せず、前回のデータを残す
- `YAHOO_CLIENT_ID` が未設定なら取得しない

| 変数 | 既定 | 説明 |
| --- | --- | --- |
| `WEATHER_REFRESH_ENABLED` | `true` | `false` でバッチを止める（テストでは止めている） |
| `WEATHER_REFRESH_INITIAL_DELAY` | `PT30S` | 起動から 1 回目までの待ち時間（ISO-8601） |
| `WEATHER_REFRESH_INTERVAL` | `PT15M` | 取得の間隔（ISO-8601） |

動いているかはログで確かめる（`天気を保存した（1 地点）` / `天気を取得できなかったため、前回のデータを残す`）。

```bash
docker logs kobe-backend 2>&1 | grep 天気
```

今は EC2 1 台で動かすため、二重に実行されることはない。台数を増やすときは、1 台だけで動くようにする仕組み（ロックなど）が必要。

## クレジット表示・利用条件

Yahoo! のクレジット表示（https://developer.yahoo.co.jp/attribution/ ）と商用利用の条件は #234 で確認する。
