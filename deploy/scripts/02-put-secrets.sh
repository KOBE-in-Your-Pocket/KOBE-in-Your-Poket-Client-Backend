#!/usr/bin/env bash
# Step 2: Secrets Manager に SUPABASE_* / DB URL を登録（図⑪ Secrets → 注入）
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"

require_aws

missing=()
for k in \
  SPRING_DATASOURCE_URL \
  SPRING_DATASOURCE_USERNAME \
  SPRING_DATASOURCE_PASSWORD \
  SUPABASE_URL \
  SUPABASE_ANON_KEY \
  SUPABASE_SERVICE_ROLE_KEY \
  SUPABASE_JWT_SECRET
do
  if [[ -z "${!k:-}" ]]; then
    missing+=("${k}")
  fi
done
if ((${#missing[@]} > 0)); then
  echo ".env に未設定があります: ${missing[*]}" >&2
  echo "cp .env.example .env して値を埋めてください。" >&2
  exit 1
fi

# JSON を安全に組み立て（python でエスケープ）
SECRET_JSON="$(
  SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}" \
  SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME}" \
  SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}" \
  SUPABASE_URL="${SUPABASE_URL}" \
  SUPABASE_ANON_KEY="${SUPABASE_ANON_KEY}" \
  SUPABASE_SERVICE_ROLE_KEY="${SUPABASE_SERVICE_ROLE_KEY}" \
  SUPABASE_JWT_SECRET="${SUPABASE_JWT_SECRET}" \
  SERVER_PORT="${SERVER_PORT}" \
  MEDIA_S3_BUCKET="${MEDIA_S3_BUCKET:-kobe-in-your-pocket-media}" \
  MEDIA_S3_REGION="${MEDIA_S3_REGION:-ap-northeast-1}" \
  MEDIA_S3_PUBLIC_BASE_URL="${MEDIA_S3_PUBLIC_BASE_URL:-}" \
  python3 - <<'PY'
import json, os
keys = [
  "SPRING_DATASOURCE_URL",
  "SPRING_DATASOURCE_USERNAME",
  "SPRING_DATASOURCE_PASSWORD",
  "SUPABASE_URL",
  "SUPABASE_ANON_KEY",
  "SUPABASE_SERVICE_ROLE_KEY",
  "SUPABASE_JWT_SECRET",
  "SERVER_PORT",
  "MEDIA_S3_BUCKET",
  "MEDIA_S3_REGION",
  "MEDIA_S3_PUBLIC_BASE_URL",
]
print(json.dumps({k: os.environ[k] for k in keys}))
PY
)"

echo_step "Secrets Manager: ${SECRET_NAME}"
if aws_cli secretsmanager describe-secret --secret-id "${SECRET_NAME}" >/dev/null 2>&1; then
  aws_cli secretsmanager put-secret-value \
    --secret-id "${SECRET_NAME}" \
    --secret-string "${SECRET_JSON}" >/dev/null
  echo "更新しました: ${SECRET_NAME}"
else
  aws_cli secretsmanager create-secret \
    --name "${SECRET_NAME}" \
    --description "KOBE backend prod (Supabase + JDBC)" \
    --secret-string "${SECRET_JSON}" \
    --tags "Key=Project,Value=${PROJECT_NAME}" >/dev/null
  echo "作成しました: ${SECRET_NAME}"
fi

save_state SECRET_NAME "${SECRET_NAME}"

echo ""
echo "次: ./scripts/03-bootstrap-ec2.sh"
echo "（EC2 に compose / deploy スクリプトを置き、Secrets を注入）"
