#!/usr/bin/env bash
# 共通ヘルパー。各スクリプトから source する。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="${ROOT_DIR}/state"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "${ROOT_DIR}/.env"
  set +a
fi

AWS_REGION="${AWS_REGION:-ap-northeast-1}"
PROJECT_NAME="${PROJECT_NAME:-kobe-backend}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t4g.small}"
EBS_SIZE_GB="${EBS_SIZE_GB:-10}"
APP_PORT="${APP_PORT:-9090}"
SSH_CIDR="${SSH_CIDR:-0.0.0.0/0}"
SECRET_NAME="${SECRET_NAME:-kobe/backend/prod}"
SERVER_PORT="${SERVER_PORT:-9090}"

# 開発は false（EC2 上の app.env）。本番は true（Secrets Manager）
USE_SECRETS_MANAGER="${USE_SECRETS_MANAGER:-false}"

# 稼働スケジュール（EventBridge Scheduler / Asia/Tokyo）。後から 05-schedule-ec2.sh で変更可
SCHEDULE_ENABLED="${SCHEDULE_ENABLED:-true}"
SCHEDULE_TIMEZONE="${SCHEDULE_TIMEZONE:-Asia/Tokyo}"
SCHEDULE_START_CRON="${SCHEDULE_START_CRON:-cron(0 9 ? * MON-FRI *)}"
# 停止は毎日。起動は平日だけだが、CD や手動で時間外に起動した分を必ず落とすため
# （停止も MON-FRI にすると、金曜夜に起動した EC2 が月曜 18:00 まで動き続ける）
SCHEDULE_STOP_CRON="${SCHEDULE_STOP_CRON:-cron(0 18 ? * * *)}"
DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/arm64}"

aws_cli() {
  if [[ -n "${AWS_PROFILE:-}" ]]; then
    aws --profile "${AWS_PROFILE}" --region "${AWS_REGION}" "$@"
  else
    # 空の AWS_PROFILE= が .env にあると profile "" になるため明示的に外す
    unset AWS_PROFILE
    aws --region "${AWS_REGION}" "$@"
  fi
}

require_aws() {
  if ! command -v aws >/dev/null 2>&1; then
    echo "aws CLI がありません。https://docs.aws.amazon.com/cli/ を参照" >&2
    exit 1
  fi
  if ! aws_cli sts get-caller-identity >/dev/null 2>&1; then
    echo "AWS 認証に失敗しました。aws configure または aws login / aws sso login を実行してください。" >&2
    exit 1
  fi
}

account_id() {
  aws_cli sts get-caller-identity --query Account --output text
}

save_state() {
  local key="$1"
  local value="$2"
  # cron(...) など特殊文字を含む値はシングルクォートで保存
  local stored="${value}"
  if [[ "${value}" == *'('* || "${value}" == *' '* || "${value}" == *'*'* ]]; then
    stored="'${value}'"
  fi
  mkdir -p "${STATE_DIR}"
  local file="${STATE_DIR}/provision.env"
  touch "${file}"
  if grep -q "^${key}=" "${file}" 2>/dev/null; then
    if sed --version >/dev/null 2>&1; then
      sed -i "s|^${key}=.*|${key}=${stored}|" "${file}"
    else
      sed -i '' "s|^${key}=.*|${key}=${stored}|" "${file}"
    fi
  else
    echo "${key}=${stored}" >>"${file}"
  fi
}
load_state() {
  local file="${STATE_DIR}/provision.env"
  if [[ -f "${file}" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "${file}"
    set +a
  fi
}

echo_step() {
  echo ""
  echo "==> $*"
}

# ローカル .env のアプリ用キーから EC2 用 app.env 本文を標準出力する
render_app_env() {
  # Docker Compose の env_file はシェルではない。値は生のまま書く（クォート禁止）。
  # & や / もそのまま渡せる。
  cat <<APPENV
SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-}
SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-}
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-}
SUPABASE_URL=${SUPABASE_URL:-}
SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY:-}
SUPABASE_SERVICE_ROLE_KEY=${SUPABASE_SERVICE_ROLE_KEY:-}
SUPABASE_JWT_SECRET=${SUPABASE_JWT_SECRET:-}
SERVER_PORT=${SERVER_PORT:-9090}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-supabase}
MEDIA_S3_BUCKET=${MEDIA_S3_BUCKET:-kobe-in-your-pocket-media}
MEDIA_S3_REGION=${MEDIA_S3_REGION:-ap-northeast-1}
MEDIA_S3_PUBLIC_BASE_URL=${MEDIA_S3_PUBLIC_BASE_URL:-}
APPENV
}
