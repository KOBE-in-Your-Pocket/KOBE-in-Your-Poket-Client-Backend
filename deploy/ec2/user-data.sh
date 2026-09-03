#!/usr/bin/env bash
# cloud-init: Docker とアプリ配置ディレクトリを用意（t4g / aarch64）
set -euxo pipefail

dnf update -y
dnf install -y docker jq awscli
systemctl enable --now docker
usermod -aG docker ec2-user

ARCH="$(uname -m)"
case "${ARCH}" in
  aarch64|arm64) COMPOSE_ARCH=aarch64 ;;
  x86_64|amd64) COMPOSE_ARCH=x86_64 ;;
  *) echo "unsupported arch: ${ARCH}" >&2; exit 1 ;;
esac

# docker compose v2（プラグイン）。無い場合はスタンドアロンを入れる
if ! docker compose version >/dev/null 2>&1; then
  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-${COMPOSE_ARCH}" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

mkdir -p /opt/kobe-backend
chown ec2-user:ec2-user /opt/kobe-backend

# 後続の bootstrap / deploy がここにファイルを置く
cat >/etc/profile.d/kobe-backend.sh <<'EOF'
export KOBE_APP_DIR=/opt/kobe-backend
EOF
