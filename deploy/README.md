# deploy — 開発環境（EC2 + Supabase）の構築・運用

開発用の Backend 実行環境を AWS 上に用意するためのスクリプト一式。
CD（`.github/workflows/deploy.yml`）が動かす対象そのものの構成をここで作る。

```
開発者 push → GitHub Actions → ECR → SSM Run Command → EC2 (docker compose)
                                                         ├─ Supabase Auth
                                                         ├─ Supabase Postgres (session :5432)
                                                         └─ S3（画像）
```

これまで手元にしか無く、変更履歴もレビューも残らなかった。実際、EventBridge Scheduler の
IAM ポリシーが**古いインスタンス ID を指したまま 7 週間気づかれず、EC2 が停止せずに
課金され続けた**事故が起きている。構築物をリポジトリに置いて追跡できるようにする。

## ディレクトリ

| パス | 内容 |
| --- | --- |
| `ec2/compose.yaml` | EC2 上で動く compose（DB は持たず Supabase を見る） |
| `ec2/deploy.sh` | EC2 上で実行される更新スクリプト（CD が SSM 経由で叩く） |
| `ec2/fetch-secrets.sh` | Secrets Manager から `app.env` を生成する |
| `ec2/user-data.sh` | インスタンス初期化（docker 導入・自動起動の有効化） |
| `iam/*.json` | EC2 / GitHub Actions に付与する IAM ポリシー |
| `scripts/*.sh` | 構築手順（下記） |
| `state/`（**git 管理外**） | 秘密鍵・`provision.env`・イメージ tar |

## 構築の順序

```bash
cd deploy
./scripts/00-check-aws.sh        # 認証・リージョンの確認
./scripts/01-provision-ec2.sh    # VPC/SG/EC2/EIP/IAM を作成
./scripts/02-put-secrets.sh      # Secrets Manager へ登録（USE_SECRETS_MANAGER=true のとき）
./scripts/03-bootstrap-ec2.sh    # EC2 に compose と app.env を配置
./scripts/04-deploy-remote.sh    # 手動デプロイ（通常は CD が行う）
./scripts/05-schedule-ec2.sh     # 起動・停止スケジュール
./scripts/status.sh              # 現在の状態
```

各スクリプトは `state/provision.env` を読み書きする。**このファイルと `state/*.pem` は
git 管理外**（`.gitignore`）なので、引き継ぐときは別途安全な経路で渡すこと。

## 稼働スケジュール

コスト削減のため、開発環境の EC2 は常時稼働させない。

| | 設定 | 理由 |
| --- | --- | --- |
| 起動 | `cron(0 9 ? * MON-FRI *)` | 平日の作業時間に合わせる |
| 停止 | `cron(0 18 ? * * *)` | **毎日**。起動は平日だけだが、CD や手動で時間外に起動した分を必ず落とすため |
| タイムゾーン | `Asia/Tokyo` | |

停止を `MON-FRI` にすると、金曜夜に起動した EC2 が月曜 18:00 まで動き続ける。

時間を変えるときは `state/provision.env` の `SCHEDULE_START_CRON` / `SCHEDULE_STOP_CRON`
を編集して `./scripts/05-schedule-ec2.sh` を再実行する（upsert）。

### インスタンスを作り直したら 05 を必ず再実行する

`05-schedule-ec2.sh` が作る IAM ポリシーは**インスタンス ID を直接指定**している。
インスタンスを作り直した場合、再実行しないと停止が権限エラーで失敗し続ける。
スケジューラーは失敗しても画面上は `ENABLED` のままなので気づきにくい。

確認方法:

```bash
aws iam get-role-policy \
  --role-name kobe-backend-ec2-scheduler-role \
  --policy-name kobe-backend-ec2-start-stop \
  --query 'PolicyDocument.Statement[0].Resource' --output text
# → 現在のインスタンス ID と一致していること
```

## 時間外に使いたいとき

GitHub Actions の `ec2-power` ワークフローから `start` / `stop` を実行できる。
ローカルの AWS 認証が切れていてもブラウザから操作できる。

コンテナは `restart: unless-stopped` と docker の自動起動で復帰するため、
EC2 を起動し直すだけでよく、デプロイのやり直しは不要。
