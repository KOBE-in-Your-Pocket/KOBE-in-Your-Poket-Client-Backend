# EC2 の app.env / Secrets Manager

CD（[`cd-ecr-ssm.md`](./cd-ecr-ssm.md)）の `deploy.sh` は、イメージの pull 後に **環境変数の用意** をしてから `compose up` する。  
入れ方は開発向けの **`app.env` 手置き** と、本番向けの **Secrets Manager 注入** の 2 通り。

`compose.yaml` はどちらも起動時に `/opt/kobe-backend/app.env` を `env_file` で読む。  
差分は **そのファイルを誰が・いつ作り、いつ消すか**。

## 現行 CD の前提（開発）

[`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) は次のように実行する。

```bash
USE_SECRETS_MANAGER=false bash /opt/kobe-backend/deploy.sh
```

このとき [`deploy/ec2/deploy.sh`](../../deploy/ec2/deploy.sh) は:

1. ECR からイメージを pull する
2. **`/opt/kobe-backend/app.env` が無ければエラー終了**（`compose up` しない）
3. あれば既存 `app.env` を使ってコンテナを起動する（**ディスク上に残す**）

したがって **初回〜開発中は、CD の前に EC2 へ `app.env` を置いておく必要がある。**

入れるキー（`SUPABASE_*` / DB URL など）は [`../supabase-env.md`](../supabase-env.md) と [`Supabase接続メモ.md`](./Supabase接続メモ.md) を参照。

### bootstrap で app.env を配置する

プロビジョニング用の `cd-ec2-supabase` で:

```bash
# USE_SECRETS_MANAGER=false（既定）のとき、ローカル .env から app.env を送る
./scripts/03-bootstrap-ec2.sh
```

- 配置先: `/opt/kobe-backend/app.env`（権限 `600`）
- あわせて `compose.yaml` / `deploy.sh` / `fetch-secrets.sh` も `/opt/kobe-backend` に置く
- **削除しない**（開発中は手置きファイルとして残す）

`app.env` が欠けたまま CD だけ走ると、SSM 上で `app.env がありません` で失敗する。

## Secrets Manager を有効化する（本番寄り）

Secrets を正とし、**ホスト上に平文を残さない**運用にする場合:

1. **シークレット登録**（既定名 `kobe/backend/prod`）

   ```bash
   # cd-ec2-supabase
   ./scripts/02-put-secrets.sh
   ```

2. **EC2 ロール**に `secretsmanager:GetSecretValue` 等を付与（プロビジョニング時 `USE_SECRETS_MANAGER=true` で付く想定）

3. **bootstrap** で `fetch-secrets.sh` を配置する

   ```bash
   USE_SECRETS_MANAGER=true ./scripts/03-bootstrap-ec2.sh
   ```

4. **CD の SSM コマンド**を変更する

   ```bash
   # 今（開発）— app.env をディスクに残す
   USE_SECRETS_MANAGER=false bash /opt/kobe-backend/deploy.sh

   # 本番 — 一時ファイル経由で注入し、up 後に削除
   USE_SECRETS_MANAGER=true SECRET_NAME=kobe/backend/prod \
     bash /opt/kobe-backend/deploy.sh
   ```

### 保存先と削除タイミング（Secrets モード）

| 時点 | `/opt/kobe-backend/app.env` |
| --- | --- |
| `fetch-secrets.sh` 実行直後 | **作成**（`chmod 600`）。Secrets Manager の JSON を `KEY=VALUE` に展開 |
| `docker compose up` | `env_file` 経由で **コンテナの環境変数へ取り込み** |
| `compose up` 成功直後 | **`deploy.sh` が `rm -f` で削除**（ホストに平文を残さない） |

補足:

- Compose の `env_file` は「起動時の受け渡し」に使い、Secrets モードでは **永続ストアにしない**
- コンテナ再起動（`restart: unless-stopped`）は、作成時に取り込んだ環境変数を使うため、削除後も稼働できる
- 再デプロイのたびに Secrets → 一時 `app.env` → up → 削除、を繰り返す
- 開発モード（`USE_SECRETS_MANAGER=false`）では削除しない
