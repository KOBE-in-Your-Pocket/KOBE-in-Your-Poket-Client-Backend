#!/usr/bin/env bash
# 状態表示
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"
load_state

echo "project=${PROJECT_NAME}"
echo "region=${AWS_REGION}"
echo "instance_type=${INSTANCE_TYPE}"
echo "docker_platform=${DOCKER_PLATFORM}"
echo "secret=${SECRET_NAME}"
echo "instance=${INSTANCE_ID:-"(未作成)"}"
echo "eip=${ELASTIC_IP:-"(未作成)"}"
echo "pem=${PEM_PATH:-"(未作成)"}"
echo "schedule_start=${SCHEDULE_START_CRON:-"(未設定)"}"
echo "schedule_stop=${SCHEDULE_STOP_CRON:-"(未設定)"}"
echo "schedule_tz=${SCHEDULE_TIMEZONE:-Asia/Tokyo}"
if [[ -n "${ELASTIC_IP:-}" ]]; then
  echo "health=http://${ELASTIC_IP}:${APP_PORT}/actuator/health"
  echo "spots=http://${ELASTIC_IP}:${APP_PORT}/api/v1/tourism/spots"
fi
