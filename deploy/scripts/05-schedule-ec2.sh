#!/usr/bin/env bash
# Step 5: 平日 9:00 起動 / 18:00 停止（土日は止めたまま）
# EventBridge Scheduler + IAM。時間は .env の SCHEDULE_* を変えて再実行すれば更新できる。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"

require_aws
load_state

if [[ "${SCHEDULE_ENABLED}" != "true" ]]; then
  echo "SCHEDULE_ENABLED=false のためスキップします。"
  exit 0
fi

if [[ -z "${INSTANCE_ID:-}" ]]; then
  echo "INSTANCE_ID がありません。先に ./scripts/01-provision-ec2.sh を実行してください。" >&2
  exit 1
fi

SCHEDULER_ROLE_NAME="${PROJECT_NAME}-ec2-scheduler-role"
START_NAME="${PROJECT_NAME}-ec2-start"
STOP_NAME="${PROJECT_NAME}-ec2-stop"
ACCOUNT_ID="$(account_id)"
ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${SCHEDULER_ROLE_NAME}"

echo_step "スケジュール設定"
echo "timezone=${SCHEDULE_TIMEZONE}"
echo "start=${SCHEDULE_START_CRON}"
echo "stop=${SCHEDULE_STOP_CRON}"
echo "instance=${INSTANCE_ID}"

echo_step "Scheduler 用 IAM ロール"
if ! aws_cli iam get-role --role-name "${SCHEDULER_ROLE_NAME}" >/dev/null 2>&1; then
  aws_cli iam create-role \
    --role-name "${SCHEDULER_ROLE_NAME}" \
    --assume-role-policy-document "{
      \"Version\": \"2012-10-17\",
      \"Statement\": [{
        \"Effect\": \"Allow\",
        \"Principal\": {\"Service\": \"scheduler.amazonaws.com\"},
        \"Action\": \"sts:AssumeRole\"
      }]
    }" >/dev/null
fi

aws_cli iam put-role-policy \
  --role-name "${SCHEDULER_ROLE_NAME}" \
  --policy-name "${PROJECT_NAME}-ec2-start-stop" \
  --policy-document "{
    \"Version\": \"2012-10-17\",
    \"Statement\": [{
      \"Effect\": \"Allow\",
      \"Action\": [\"ec2:StartInstances\", \"ec2:StopInstances\"],
      \"Resource\": \"arn:aws:ec2:${AWS_REGION}:${ACCOUNT_ID}:instance/${INSTANCE_ID}\"
    }]
  }"

# ロール伝播待ち（初回作成時）
sleep 8

upsert_schedule() {
  local name="$1"
  local cron="$2"
  local api_action="$3" # startInstances | stopInstances
  local target_file
  target_file="$(mktemp)"

  python3 - "${INSTANCE_ID}" "${ROLE_ARN}" "${api_action}" "${target_file}" <<'PY'
import json, sys
instance_id, role_arn, api_action, path = sys.argv[1:5]
target = {
    "Arn": f"arn:aws:scheduler:::aws-sdk:ec2:{api_action}",
    "RoleArn": role_arn,
    "Input": json.dumps({"InstanceIds": [instance_id]}),
}
with open(path, "w", encoding="utf-8") as f:
    json.dump(target, f)
PY

  if aws_cli scheduler get-schedule --name "${name}" >/dev/null 2>&1; then
    aws_cli scheduler update-schedule \
      --name "${name}" \
      --schedule-expression "${cron}" \
      --schedule-expression-timezone "${SCHEDULE_TIMEZONE}" \
      --flexible-time-window '{"Mode":"OFF"}' \
      --target "file://${target_file}" \
      --state ENABLED >/dev/null
    echo "更新: ${name}"
  else
    aws_cli scheduler create-schedule \
      --name "${name}" \
      --schedule-expression "${cron}" \
      --schedule-expression-timezone "${SCHEDULE_TIMEZONE}" \
      --flexible-time-window '{"Mode":"OFF"}' \
      --target "file://${target_file}" \
      --state ENABLED >/dev/null
    echo "作成: ${name}"
  fi
  rm -f "${target_file}"
}

echo_step "EventBridge Scheduler 作成/更新"
upsert_schedule "${START_NAME}" "${SCHEDULE_START_CRON}" "startInstances"
upsert_schedule "${STOP_NAME}" "${SCHEDULE_STOP_CRON}" "stopInstances"

save_state SCHEDULER_ROLE_NAME "${SCHEDULER_ROLE_NAME}"
save_state SCHEDULE_START_NAME "${START_NAME}"
save_state SCHEDULE_STOP_NAME "${STOP_NAME}"
save_state SCHEDULE_START_CRON "${SCHEDULE_START_CRON}"
save_state SCHEDULE_STOP_CRON "${SCHEDULE_STOP_CRON}"
save_state SCHEDULE_TIMEZONE "${SCHEDULE_TIMEZONE}"

echo ""
echo "========================================"
echo " スケジュール設定完了（${SCHEDULE_TIMEZONE}）"
echo "  月〜金 起動: ${SCHEDULE_START_CRON}"
echo "  月〜金 停止: ${SCHEDULE_STOP_CRON}"
echo "  土日は起動スケジュール無し（止めたまま）"
echo "========================================"
echo "時間を変えるとき:"
echo "  1. .env の SCHEDULE_START_CRON / SCHEDULE_STOP_CRON を編集"
echo "  2. ./scripts/05-schedule-ec2.sh を再実行"
echo ""
echo "例: 10:00 起動 / 19:00 停止"
echo "  SCHEDULE_START_CRON='cron(0 10 ? * MON-FRI *)'"
echo "  SCHEDULE_STOP_CRON='cron(0 19 ? * MON-FRI *)'"
