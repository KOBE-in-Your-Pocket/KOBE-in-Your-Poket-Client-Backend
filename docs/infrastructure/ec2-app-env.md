# EC2 の app.env / Secrets Manager

CD（[`cd-ecr-ssm.md`](./cd-ecr-ssm.md)）の `deploy.sh` は、イメージの pull 後に **環境変数の用意** をしてから `compose up` する。  
入れ方は開発向けの **`app.env` 手置き** と、本番向けの **Secrets Manager 注入** の 2 通り。

## 現行 CD の前提（開発）

[`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) は次のように実行する。

```bash
USE_SECRETS_MANAGER=false bash /opt/kobe-backend/deploy.sh
```

このとき [`deploy/ec2/deploy.sh`](../../deploy/ec2/deploy.sh) は:

1. ECR からイメージを pull する
2. **`/opt/kobe-backend/app.env` が無ければエラー終了**（`compose up` しない）
3. あれば既存 `app.env` を使ってコンテナを起動する

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

`app.env` が欠けたまま CD だけ走ると、SSM 上で `app.env がありません` で失敗する。

## Secrets Manager を有効化する（本番寄り）

ディスク上の平文 `app.env` をやめ、起動時に Secrets から生成する場合:

1. **シークレット登録**（既定名 `kobe/backend/prod`）

   ```bash
   # cd-ec2-supabase
   ./scripts/02-put-secrets.sh
   ```

2. **EC2 ロール**に `secretsmanager:GetSecretValue` 等を付与（プロビジョニング時 `USE_SECRETS_MANAGER=true` で付く想定）

3. **bootstrap** で `fetch-secrets.sh` を配置したうえで、一度注入を確認する

   ```bash
   USE_SECRETS_MANAGER=true ./scripts/03-bootstrap-ec2.sh
   ```

4. **CD の SSM コマンド**を変更する

   ```bash
   # 今（開発）
   USE_SECRETS_MANAGER=false bash /opt/kobe-backend/deploy.sh

   # 本番
   USE_SECRETS_MANAGER=true SECRET_NAME=kobe/backend/prod \
     bash /opt/kobe-backend/deploy.sh
   ```

`USE_SECRETS_MANAGER=true` のとき `deploy.sh` は `fetch-secrets.sh` で `app.env` を生成してから `compose up` する。
