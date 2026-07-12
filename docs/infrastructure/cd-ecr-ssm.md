# CD 構成（ECR + SSM）

> workflow: [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml)

SSH（22番）をインターネットに開けず、GitHub Actions から EC2 へデプロイするための構成です。

## 全体像

```text
開発者 push → GitHub (develop)
                 → GitHub Actions（arm64 ビルド）
                      │
                      ├─ OIDC で AWS 一時認証
                      ├─ docker buildx --push → ECR
                      └─ SSM Run Command で EC2 に命令
                              │
                              ▼
                     EC2 が ECR から pull → compose 再起動
                              │
                              ▼
                     Health check（HTTP :9090）
```

## なぜこの構成か

| 方式 | 内容 |
| --- | --- |
| SSH + SG で Actions IP を許可 | 可能だが、Actions の出口 IP が多数・変動する |
| SSH を `0.0.0.0/0` | 入口を世界に晒すため非推奨 |
| S3 + `docker save`/`load` + SSM | 以前の方式。tar 置き場として S3 を使っていた |
| **ECR + SSM（採用）** | コンテナイメージの標準的な配布。22番不要。OIDC の一時権限 |

最初は SSH で tar を送り、次に置き場だけ S3 にした。  
イメージ配布としては ECR が本命のため、`docker push` / `pull` に移行した。

## 主な AWS リソース

| リソース | 名前 / 値 |
| --- | --- |
| ECR | `515966496540.dkr.ecr.ap-northeast-1.amazonaws.com/kobe-backend` |
| IAM Role（Actions） | `kobe-backend-github-actions-deploy`（OIDC・ECR push・SSM） |
| IAM Role（EC2） | `kobe-backend-ec2-role`（ECR pull）+ `AmazonSSMManagedInstanceCore` |
| EC2 | `i-0d0c06a6ced085c4d`（`t4g.small` / arm64） |
| Elastic IP | `18.181.34.28` |

## GitHub 側で必要なもの

- **OIDC 用:** workflow の `permissions: id-token: write`（実装済み）
- **不要:** `EC2_SSH_KEY` / `EC2_SSH_USER`（SSH しないため）

## EC2 上の deploy.sh

`/opt/kobe-backend/deploy.sh`（リポジトリ上は [`deploy/ec2/deploy.sh`](../../deploy/ec2/deploy.sh)）は、`APP_IMAGE` が ECR URI のとき login → pull → `compose up` する。  
旧来の tar 引数（`docker load`）も互換のため残している。

### app.env / Secrets Manager の前提

現行 workflow は **`USE_SECRETS_MANAGER=false`** で `deploy.sh` を実行する。  
このモードでは **`/opt/kobe-backend/app.env` が無いと `compose up` 前に終了**する。

| 段階 | やり方 | 詳細 |
| --- | --- | --- |
| **開発（現行 CD）** | EC2 bootstrap で `app.env` を事前配置 | [`ec2-app-env.md`](./ec2-app-env.md#bootstrap-で-appenv-を配置する) |
| **本番寄り** | Secrets Manager を有効化し、CD で `USE_SECRETS_MANAGER=true`（一時 `app.env` → up 後削除） | [`ec2-app-env.md`](./ec2-app-env.md#secrets-manager-を有効化する本番寄り) |

環境変数のキー一覧は [`../supabase-env.md`](../supabase-env.md) を参照。
