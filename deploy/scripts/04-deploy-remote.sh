#!/usr/bin/env bash
# Step 4: ローカルで Docker イメージをビルド → EC2 へ scp → docker load & restart
# 図⑪: SSH / scp jar（ここでは docker image tar）
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"

load_state

BACKEND_DIR="${BACKEND_DIR:-${ROOT_DIR}/../KOBE-in-Your-Poket-Client-Backend}"
IMAGE_NAME="${IMAGE_NAME:-kobe-backend}"
IMAGE_TAG="${IMAGE_TAG:-$(date +%Y%m%d%H%M%S)}"
FULL_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
TAR_PATH="${STATE_DIR}/${IMAGE_NAME}-${IMAGE_TAG}.tar"

if [[ -z "${ELASTIC_IP:-}" || -z "${PEM_PATH:-}" ]]; then
  echo "先に 01-provision-ec2.sh を実行してください。" >&2
  exit 1
fi
if [[ ! -d "${BACKEND_DIR}" ]]; then
  echo "backend が見つかりません: ${BACKEND_DIR}" >&2
  echo "BACKEND_DIR=/path/to/KOBE-in-Your-Poket-Client-Backend で指定できます。" >&2
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker が必要です（ローカルビルド用）。" >&2
  exit 1
fi

SSH=(ssh -i "${PEM_PATH}" -o StrictHostKeyChecking=accept-new)
SCP=(scp -i "${PEM_PATH}" -o StrictHostKeyChecking=accept-new)
HOST="ec2-user@${ELASTIC_IP}"
REMOTE_DIR="/opt/kobe-backend"

echo_step "Docker ビルド: ${FULL_IMAGE} (${DOCKER_PLATFORM})"
docker build --platform "${DOCKER_PLATFORM}" \
  -t "${FULL_IMAGE}" -t "${IMAGE_NAME}:latest" "${BACKEND_DIR}"

echo_step "イメージを tar に保存"
docker save "${FULL_IMAGE}" -o "${TAR_PATH}"
ls -lh "${TAR_PATH}"

echo_step "EC2 へ転送"
"${SCP[@]}" "${TAR_PATH}" "${HOST}:${REMOTE_DIR}/image.tar"

echo_step "docker load & restart"
"${SSH[@]}" "${HOST}" \
  "APP_IMAGE='${FULL_IMAGE}' SECRET_NAME='${SECRET_NAME}' AWS_REGION='${AWS_REGION}' \
   USE_SECRETS_MANAGER='${USE_SECRETS_MANAGER}' \
   bash ${REMOTE_DIR}/deploy.sh ${REMOTE_DIR}/image.tar"

echo_step "ヘルスチェック"
sleep 8
if curl -fsS "http://${ELASTIC_IP}:${APP_PORT}/actuator/health"; then
  echo ""
  echo "OK: http://${ELASTIC_IP}:${APP_PORT}/actuator/health"
else
  echo ""
  echo "health がまだ UP でない可能性があります。ログを確認:"
  echo "  ssh -i ${PEM_PATH} ${HOST} 'docker logs kobe-backend --tail 100'"
  exit 1
fi

echo ""
echo "スポット一覧の確認例:"
echo "  curl -sS http://${ELASTIC_IP}:${APP_PORT}/api/v1/tourism/spots | head"
