#!/usr/bin/env bash
# EC2 の中から backend の /actuator/health を確認する（deploy.yml / ec2-power.yml から呼ぶ）。
#
# backend の 9090 番は EC2 の localhost にだけ公開しているため（#221）、GitHub Actions の
# ランナーから直接は叩けない。SSM Run Command で EC2 の中から localhost を叩く。
#
# 必要な環境変数: EC2_INSTANCE_ID, APP_PORT（AWS 認証と AWS_REGION は呼び出し側で設定済みのこと）
set -euo pipefail

: "${EC2_INSTANCE_ID:?EC2_INSTANCE_ID が必要です}"
: "${APP_PORT:?APP_PORT が必要です}"

# 起動直後は SSM エージェントがまだ繋がっていないことがある。
for i in $(seq 1 36); do
  PING=$(aws ssm describe-instance-information \
    --filters "Key=InstanceIds,Values=${EC2_INSTANCE_ID}" \
    --query 'InstanceInformationList[0].PingStatus' --output text 2>/dev/null || echo None)
  echo "SSM ping=${PING}"
  [[ "${PING}" == "Online" ]] && break
  if [[ "${i}" -eq 36 ]]; then
    echo "SSM エージェントが Online にならない" >&2
    exit 1
  fi
  sleep 5
done

# EC2 側でも最大 3 分（5 秒 × 36 回）待つ。アプリの起動には 1 分ほどかかる。
REMOTE_SCRIPT="for i in \$(seq 1 36); do if curl -fsS http://localhost:${APP_PORT}/actuator/health; then echo; echo 'health OK'; exit 0; fi; sleep 5; done; echo 'health check failed' >&2; exit 1"
PARAMS=$(mktemp)
python3 - "${PARAMS}" "${REMOTE_SCRIPT}" <<'PY'
import json, sys
json.dump({"commands": [sys.argv[2]]}, open(sys.argv[1], "w"))
PY

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "${EC2_INSTANCE_ID}" \
  --document-name "AWS-RunShellScript" \
  --comment "health check" \
  --timeout-seconds 300 \
  --parameters "file://${PARAMS}" \
  --query 'Command.CommandId' --output text)
echo "COMMAND_ID=${COMMAND_ID}"

STATUS=Pending
for i in $(seq 1 40); do
  sleep 5
  STATUS=$(aws ssm get-command-invocation \
    --command-id "${COMMAND_ID}" \
    --instance-id "${EC2_INSTANCE_ID}" \
    --query 'Status' --output text 2>/dev/null || echo Pending)
  case "${STATUS}" in
    Success | Failed | Cancelled | TimedOut) break ;;
  esac
done

aws ssm get-command-invocation \
  --command-id "${COMMAND_ID}" \
  --instance-id "${EC2_INSTANCE_ID}" \
  --query '[StandardOutputContent,StandardErrorContent]' --output text || true

[[ "${STATUS}" == "Success" ]]
