#!/usr/bin/env bash
# EC2 上で実行: Secrets Manager から app.env を生成する（図⑪の「注入」）
set -euo pipefail

SECRET_NAME="${SECRET_NAME:-kobe/backend/prod}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
APP_DIR="${KOBE_APP_DIR:-/opt/kobe-backend}"
OUT_ENV="${APP_DIR}/app.env"

mkdir -p "${APP_DIR}"

JSON="$(aws secretsmanager get-secret-value \
  --secret-id "${SECRET_NAME}" \
  --region "${AWS_REGION}" \
  --query SecretString --output text)"

# jq で KEY=VALUE に展開（値に改行が無い前提）
echo "${JSON}" | jq -r 'to_entries[] | "\(.key)=\(.value)"' >"${OUT_ENV}"
chmod 600 "${OUT_ENV}"
echo "wrote ${OUT_ENV}"
