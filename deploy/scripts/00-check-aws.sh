#!/usr/bin/env bash
# Step 0: AWS CLI 認証確認
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"

require_aws

echo "== AWS CLI =="
aws --version
echo ""
echo "== Caller identity =="
aws_cli sts get-caller-identity --output table
echo ""
echo "認証 OK（region=${AWS_REGION} project=${PROJECT_NAME}）"
echo "次: cp .env.example .env を埋めたら ./scripts/01-provision-ec2.sh"
