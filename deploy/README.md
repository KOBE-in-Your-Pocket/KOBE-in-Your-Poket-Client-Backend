# deploy — EC2 上の実行環境

CD（`.github/workflows/deploy.yml`）がデプロイする先の構成。

```
インターネット ──HTTPS(443)──> [ EC2 ]
                                Caddy が TLS 終端
                                  │ HTTP(9090)  ← EC2 内部で完結するため平文でよい
                                  ▼
                             Spring Boot アプリ ──> Supabase (Auth / Postgres)
```

## ファイル

| パス | 役割 | EC2 への反映 |
| --- | --- | --- |
| `ec2/compose.yaml` | app と Caddy の定義 | **CD が同期** |
| `ec2/Caddyfile` | TLS 終端と公開パスの制限 | **CD が同期** |
| `ec2/deploy.sh` | EC2 上で compose を起動する | **CD が同期** |
| `ec2/fetch-secrets.sh` | Secrets Manager から `app.env` を生成 | 同期対象外（初期構築時に配置） |
| `ec2/user-data.sh` | インスタンス初期化（docker 導入・自動起動） | 起動時に一度だけ実行 |

配置先は EC2 の `/opt/kobe-backend/`。

## CD がやること

`deploy.yml` の `Sync EC2 config from repo` ステップが、`deploy/ec2/` の
`compose.yaml` / `Caddyfile` / `deploy.sh` を SSM 経由で EC2 へ書き込む。
そのあと `Deploy via SSM` が `deploy.sh` を実行する。

**このリポジトリで上記 3 ファイルを変更すれば、次のデプロイで EC2 に反映される。**
SSH は使わない（22 番をインターネットに開けていないため）。

`app.env`（DB・Supabase の接続情報）は秘密情報のため同期対象外。
`USE_SECRETS_MANAGER=true` なら `fetch-secrets.sh` が生成し、開発時は手動で配置する。

## HTTPS

Caddy が Let's Encrypt から証明書を自動取得・自動更新する。

- ホスト名は `18-181-34-28.sslip.io`。証明書は IP アドレスには発行されないため、
  Elastic IP をそのまま返す公開 DNS（sslip.io）を借りている
- 独自ドメイン取得後は `Caddyfile` のホスト名を差し替えるだけで移行できる
- 80 番の開放が必要（Let's Encrypt の HTTP-01 チャレンジ）
- 証明書は `caddy_data` ボリュームに保存される。**消すと再取得**になり、
  Let's Encrypt のレート制限に当たりうる
- 公開するのは `/api/*` のみ。`/actuator` は外部から 404

## 稼働スケジュール

EventBridge Scheduler で **平日 9:00 起動 / 毎日 18:00 停止（Asia/Tokyo）**。
停止だけ毎日なのは、CD や手動で時間外に起動した分を確実に落とすため。

時間外に使いたいときは、GitHub Actions の `ec2-power` ワークフローから
`start` / `stop` を実行できる。コンテナは `restart: unless-stopped` と
docker の自動起動で復帰するため、デプロイのやり直しは不要。

スケジュールを作る IAM ポリシーは**インスタンス ID を直接指定**している。
インスタンスを作り直したら再設定しないと、停止が権限エラーで失敗し続ける
（スケジューラーの表示は `ENABLED` のままなので気づきにくい）。

```bash
aws iam get-role-policy \
  --role-name kobe-backend-ec2-scheduler-role \
  --policy-name kobe-backend-ec2-start-stop \
  --query 'PolicyDocument.Statement[0].Resource' --output text
# → 現在のインスタンス ID と一致していること
```
