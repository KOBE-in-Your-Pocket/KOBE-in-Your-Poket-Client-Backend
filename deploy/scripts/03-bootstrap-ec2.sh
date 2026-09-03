#!/usr/bin/env bash
# Step 3: EC2 にアプリ配置一式を送る。開発は app.env 手置き、本番は Secrets 注入。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"

require_aws
load_state

if [[ -z "${ELASTIC_IP:-}" || -z "${PEM_PATH:-}" ]]; then
  echo "state/provision.env に ELASTIC_IP / PEM_PATH がありません。先に 01-provision-ec2.sh を実行してください。" >&2
  exit 1
fi
if [[ ! -f "${PEM_PATH}" ]]; then
  echo "秘密鍵がありません: ${PEM_PATH}" >&2
  exit 1
fi

SSH=(ssh -i "${PEM_PATH}" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15)
SCP=(scp -i "${PEM_PATH}" -o StrictHostKeyChecking=accept-new)
HOST="ec2-user@${ELASTIC_IP}"
REMOTE_DIR="/opt/kobe-backend"

echo_step "cloud-init / Docker の準備完了を待つ"
for i in $(seq 1 36); do
  if "${SSH[@]}" "${HOST}" "command -v docker >/dev/null && test -d ${REMOTE_DIR}"; then
    echo "EC2 ready"
    break
  fi
  if [[ "${i}" -eq 36 ]]; then
    echo "タイムアウト: EC2 の user-data 完了を確認できません。数分待って再実行してください。" >&2
    exit 1
  fi
  echo "waiting... (${i})"
  sleep 10
done

echo_step "スクリプト配置"
"${SSH[@]}" "${HOST}" "sudo mkdir -p ${REMOTE_DIR} && sudo chown ec2-user:ec2-user ${REMOTE_DIR}"
"${SCP[@]}" \
  "${ROOT_DIR}/ec2/compose.yaml" \
  "${ROOT_DIR}/ec2/fetch-secrets.sh" \
  "${ROOT_DIR}/ec2/deploy.sh" \
  "${HOST}:${REMOTE_DIR}/"
"${SSH[@]}" "${HOST}" "chmod +x ${REMOTE_DIR}/fetch-secrets.sh ${REMOTE_DIR}/deploy.sh"

if [[ "${USE_SECRETS_MANAGER}" == "true" ]]; then
  echo_step "Secrets 注入"
  "${SSH[@]}" "${HOST}" \
    "SECRET_NAME='${SECRET_NAME}' AWS_REGION='${AWS_REGION}' bash ${REMOTE_DIR}/fetch-secrets.sh"
else
  echo_step "開発モード: app.env をローカル .env から配置（Secrets Manager 不使用）"
  TMP_ENV="$(mktemp)"
  render_app_env >"${TMP_ENV}"
  if ! grep -q 'SPRING_DATASOURCE_URL=.\+' "${TMP_ENV}"; then
    echo "警告: SPRING_DATASOURCE_URL が空です。.env に Supabase 接続情報を入れてから再実行してください。" >&2
  fi
  "${SCP[@]}" "${TMP_ENV}" "${HOST}:${REMOTE_DIR}/app.env"
  "${SSH[@]}" "${HOST}" "chmod 600 ${REMOTE_DIR}/app.env"
  rm -f "${TMP_ENV}"
  echo "配置: ${REMOTE_DIR}/app.env"
fi

echo ""
echo "bootstrap 完了: ${HOST}:${REMOTE_DIR}"
echo "次:"
echo "  1) 手動初回デプロイ: ./scripts/04-deploy-remote.sh"
echo "  2) スケジュール: ./scripts/05-schedule-ec2.sh"
echo "  3) または GitHub Actions 用に github-actions/deploy.yml を backend へコピー"
