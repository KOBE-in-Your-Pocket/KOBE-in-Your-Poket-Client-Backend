#!/usr/bin/env bash
# EC2 上で実行: イメージを載せて再起動
# - APP_IMAGE が ECR URI のとき: login → pull → compose up
# - IMAGE_TAR が渡されたとき: docker load（旧 S3/scp 互換）
set -euo pipefail

APP_DIR="${KOBE_APP_DIR:-/opt/kobe-backend}"
IMAGE_TAR="${1:-}"
APP_IMAGE="${APP_IMAGE:-kobe-backend:latest}"
SECRET_NAME="${SECRET_NAME:-kobe/backend/prod}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
USE_SECRETS_MANAGER="${USE_SECRETS_MANAGER:-false}"

cd "${APP_DIR}"

if [[ -n "${IMAGE_TAR}" && -f "${IMAGE_TAR}" ]]; then
  echo "docker load < ${IMAGE_TAR}"
  docker load <"${IMAGE_TAR}"
elif [[ "${APP_IMAGE}" == *.dkr.ecr.*.amazonaws.com/* ]]; then
  REGISTRY="${APP_IMAGE%%/*}"
  echo "ECR login ${REGISTRY}"
  aws ecr get-login-password --region "${AWS_REGION}" \
    | docker login --username AWS --password-stdin "${REGISTRY}"
  echo "docker pull ${APP_IMAGE}"
  docker pull "${APP_IMAGE}"
fi

if [[ "${USE_SECRETS_MANAGER}" == "true" ]]; then
  export SECRET_NAME AWS_REGION
  bash "${APP_DIR}/fetch-secrets.sh"
else
  if [[ ! -f "${APP_DIR}/app.env" ]]; then
    echo "app.env がありません。開発時は 03-bootstrap-ec2.sh で配置してください。" >&2
    exit 1
  fi
  echo "既存 app.env を使用（USE_SECRETS_MANAGER=false）"
fi

export APP_IMAGE SERVER_PORT
SERVER_PORT="$(grep -E '^SERVER_PORT=' "${APP_DIR}/app.env" | cut -d= -f2- || echo 9090)"
export SERVER_PORT

docker compose -f "${APP_DIR}/compose.yaml" up -d --force-recreate --remove-orphans

echo "deployed ${APP_IMAGE}"
docker compose -f "${APP_DIR}/compose.yaml" ps
