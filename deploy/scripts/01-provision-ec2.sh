#!/usr/bin/env bash
# Step 1: EC2 + SG + Elastic IP + IAM
# 開発合意: t4g.small + EBS 10GB + Elastic IP。秘密情報は app.env（Secrets は後回し可）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_lib.sh"

require_aws
mkdir -p "${STATE_DIR}"

SG_NAME="${PROJECT_NAME}-sg"
ROLE_NAME="${PROJECT_NAME}-ec2-role"
PROFILE_NAME="${PROJECT_NAME}-ec2-profile"
EIP_TAG_NAME="${PROJECT_NAME}-eip"

echo_step "デフォルト VPC を取得"
VPC_ID="$(aws_cli ec2 describe-vpcs \
  --filters Name=isDefault,Values=true \
  --query 'Vpcs[0].VpcId' --output text)"
if [[ -z "${VPC_ID}" || "${VPC_ID}" == "None" ]]; then
  echo "デフォルト VPC がありません。コンソールで VPC を作成するか VPC_ID を指定してください。" >&2
  exit 1
fi
echo "VPC_ID=${VPC_ID}"
save_state VPC_ID "${VPC_ID}"

SUBNET_ID="$(aws_cli ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" "Name=default-for-az,Values=true" \
  --query 'Subnets[0].SubnetId' --output text)"
if [[ -z "${SUBNET_ID}" || "${SUBNET_ID}" == "None" ]]; then
  SUBNET_ID="$(aws_cli ec2 describe-subnets \
    --filters "Name=vpc-id,Values=${VPC_ID}" \
    --query 'Subnets[0].SubnetId' --output text)"
fi
echo "SUBNET_ID=${SUBNET_ID}"
save_state SUBNET_ID "${SUBNET_ID}"

echo_step "セキュリティグループ"
SG_ID="$(aws_cli ec2 describe-security-groups \
  --filters "Name=group-name,Values=${SG_NAME}" "Name=vpc-id,Values=${VPC_ID}" \
  --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || true)"
if [[ -z "${SG_ID}" || "${SG_ID}" == "None" ]]; then
  SG_ID="$(aws_cli ec2 create-security-group \
    --group-name "${SG_NAME}" \
    --description "KOBE backend API (${PROJECT_NAME})" \
    --vpc-id "${VPC_ID}" \
    --query GroupId --output text)"
  aws_cli ec2 create-tags --resources "${SG_ID}" \
    --tags "Key=Name,Value=${SG_NAME}" "Key=Project,Value=${PROJECT_NAME}"
fi
echo "SG_ID=${SG_ID}"
save_state SG_ID "${SG_ID}"

add_sg_rule() {
  local port="$1"
  local cidr="$2"
  local desc="$3"
  aws_cli ec2 authorize-security-group-ingress \
    --group-id "${SG_ID}" \
    --ip-permissions "[{\"IpProtocol\":\"tcp\",\"FromPort\":${port},\"ToPort\":${port},\"IpRanges\":[{\"CidrIp\":\"${cidr}\",\"Description\":\"${desc}\"}]}]" \
    2>/dev/null || true
}
add_sg_rule 22 "${SSH_CIDR}" "SSH"
add_sg_rule "${APP_PORT}" "0.0.0.0/0" "Spring API"

echo_step "キーペア"
if [[ -z "${KEY_NAME:-}" ]]; then
  KEY_NAME="${PROJECT_NAME}-key"
fi
PEM_PATH="${STATE_DIR}/${KEY_NAME}.pem"
if aws_cli ec2 describe-key-pairs --key-names "${KEY_NAME}" >/dev/null 2>&1; then
  echo "既存キーペアを使用: ${KEY_NAME}"
  if [[ ! -f "${PEM_PATH}" ]]; then
    echo "警告: ${PEM_PATH} がありません。既存キーの秘密鍵を手元に置いてください。" >&2
  fi
else
  aws_cli ec2 create-key-pair \
    --key-name "${KEY_NAME}" \
    --query 'KeyMaterial' --output text >"${PEM_PATH}"
  chmod 400 "${PEM_PATH}"
  echo "新規キーを保存: ${PEM_PATH}"
fi
save_state KEY_NAME "${KEY_NAME}"
save_state PEM_PATH "${PEM_PATH}"

echo_step "AMI (arm64 / t4g)"
if [[ -z "${AMI_ID:-}" ]]; then
  AMI_ID="$(aws_cli ssm get-parameters \
    --names /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64 \
    --query 'Parameters[0].Value' --output text)"
fi
echo "AMI_ID=${AMI_ID}"
echo "INSTANCE_TYPE=${INSTANCE_TYPE}"
echo "EBS_SIZE_GB=${EBS_SIZE_GB}"
save_state AMI_ID "${AMI_ID}"
save_state INSTANCE_TYPE "${INSTANCE_TYPE}"
save_state EBS_SIZE_GB "${EBS_SIZE_GB}"

echo_step "IAM ロール / インスタンスプロファイル"
if ! aws_cli iam get-role --role-name "${ROLE_NAME}" >/dev/null 2>&1; then
  aws_cli iam create-role \
    --role-name "${ROLE_NAME}" \
    --assume-role-policy-document '{
      "Version":"2012-10-17",
      "Statement":[{
        "Effect":"Allow",
        "Principal":{"Service":"ec2.amazonaws.com"},
        "Action":"sts:AssumeRole"
      }]
    }' >/dev/null
fi

TMP_POLICY="$(mktemp)"
python3 - "${USE_SECRETS_MANAGER}" "${AWS_REGION}" "$(account_id)" "${SECRET_NAME}" "${S3_BUCKET_NAME:-}" "${TMP_POLICY}" <<'PYINNER'
import json, sys
use_secrets, region, account, secret_name, s3_bucket, path = sys.argv[1:7]
stmts = []
if use_secrets == "true":
    stmts.append({
        "Sid": "SecretsRead",
        "Effect": "Allow",
        "Action": ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"],
        "Resource": f"arn:aws:secretsmanager:{region}:{account}:secret:{secret_name}*",
    })
if s3_bucket:
    stmts.append({
        "Sid": "S3Images",
        "Effect": "Allow",
        "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
        "Resource": [f"arn:aws:s3:::{s3_bucket}", f"arn:aws:s3:::{s3_bucket}/*"],
    })
if not stmts:
    stmts.append({
        "Sid": "NoOpDescribe",
        "Effect": "Allow",
        "Action": ["ec2:DescribeTags"],
        "Resource": "*",
    })
with open(path, "w", encoding="utf-8") as f:
    json.dump({"Version": "2012-10-17", "Statement": stmts}, f, indent=2)
PYINNER

aws_cli iam put-role-policy \
  --role-name "${ROLE_NAME}" \
  --policy-name "${PROJECT_NAME}-ec2-inline" \
  --policy-document "file://${TMP_POLICY}"
rm -f "${TMP_POLICY}"

if ! aws_cli iam get-instance-profile --instance-profile-name "${PROFILE_NAME}" >/dev/null 2>&1; then
  aws_cli iam create-instance-profile --instance-profile-name "${PROFILE_NAME}" >/dev/null
  aws_cli iam add-role-to-instance-profile \
    --instance-profile-name "${PROFILE_NAME}" \
    --role-name "${ROLE_NAME}"
  sleep 8
fi
save_state ROLE_NAME "${ROLE_NAME}"
save_state PROFILE_NAME "${PROFILE_NAME}"
save_state USE_SECRETS_MANAGER "${USE_SECRETS_MANAGER}"

echo_step "EC2 インスタンス"
load_state
if [[ -n "${INSTANCE_ID:-}" ]]; then
  STATE="$(aws_cli ec2 describe-instances --instance-ids "${INSTANCE_ID}" \
    --query 'Reservations[0].Instances[0].State.Name' --output text 2>/dev/null || echo missing)"
  if [[ "${STATE}" == "running" || "${STATE}" == "pending" || "${STATE}" == "stopped" ]]; then
    echo "既存インスタンスを再利用: ${INSTANCE_ID} (${STATE})"
  else
    INSTANCE_ID=""
  fi
fi

if [[ -z "${INSTANCE_ID:-}" ]]; then
  USER_DATA_FILE="${ROOT_DIR}/ec2/user-data.sh"
  BD_MAP="$(mktemp)"
  cat >"${BD_MAP}" <<BDEOF
[
  {
    "DeviceName": "/dev/xvda",
    "Ebs": {
      "VolumeSize": ${EBS_SIZE_GB},
      "VolumeType": "gp3",
      "DeleteOnTermination": true
    }
  }
]
BDEOF
  INSTANCE_ID="$(aws_cli ec2 run-instances \
    --image-id "${AMI_ID}" \
    --instance-type "${INSTANCE_TYPE}" \
    --key-name "${KEY_NAME}" \
    --security-group-ids "${SG_ID}" \
    --subnet-id "${SUBNET_ID}" \
    --iam-instance-profile "Name=${PROFILE_NAME}" \
    --block-device-mappings "file://${BD_MAP}" \
    --user-data "file://${USER_DATA_FILE}" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${PROJECT_NAME}},{Key=Project,Value=${PROJECT_NAME}}]" \
    --query 'Instances[0].InstanceId' --output text)"
  rm -f "${BD_MAP}"
  echo "作成: ${INSTANCE_ID}"
  aws_cli ec2 wait instance-running --instance-ids "${INSTANCE_ID}"
fi
save_state INSTANCE_ID "${INSTANCE_ID}"

echo_step "Elastic IP"
ALLOC_ID="${ALLOCATION_ID:-}"
PUBLIC_IP="${ELASTIC_IP:-}"
if [[ -z "${ALLOC_ID}" ]]; then
  ALLOC_ID="$(aws_cli ec2 describe-addresses \
    --filters "Name=tag:Name,Values=${EIP_TAG_NAME}" \
    --query 'Addresses[0].AllocationId' --output text 2>/dev/null || true)"
fi
if [[ -z "${ALLOC_ID}" || "${ALLOC_ID}" == "None" ]]; then
  read -r ALLOC_ID PUBLIC_IP <<<"$(aws_cli ec2 allocate-address --domain vpc \
    --query '[AllocationId,PublicIp]' --output text)"
  aws_cli ec2 create-tags --resources "${ALLOC_ID}" \
    --tags "Key=Name,Value=${EIP_TAG_NAME}" "Key=Project,Value=${PROJECT_NAME}"
else
  PUBLIC_IP="$(aws_cli ec2 describe-addresses --allocation-ids "${ALLOC_ID}" \
    --query 'Addresses[0].PublicIp' --output text)"
fi

ASSOC="$(aws_cli ec2 describe-addresses --allocation-ids "${ALLOC_ID}" \
  --query 'Addresses[0].InstanceId' --output text)"
if [[ "${ASSOC}" != "${INSTANCE_ID}" ]]; then
  aws_cli ec2 associate-address \
    --instance-id "${INSTANCE_ID}" \
    --allocation-id "${ALLOC_ID}" >/dev/null
fi
save_state ALLOCATION_ID "${ALLOC_ID}"
save_state ELASTIC_IP "${PUBLIC_IP}"

echo ""
echo "========================================"
echo " EC2 準備完了"
echo " INSTANCE_ID=${INSTANCE_ID}"
echo " ELASTIC_IP=${PUBLIC_IP}"
echo " INSTANCE_TYPE=${INSTANCE_TYPE}"
echo " EBS_SIZE_GB=${EBS_SIZE_GB}"
echo " USE_SECRETS_MANAGER=${USE_SECRETS_MANAGER}"
echo " PEM=${PEM_PATH}"
echo " API (後で): http://${PUBLIC_IP}:${APP_PORT}/actuator/health"
echo "========================================"
if [[ "${USE_SECRETS_MANAGER}" == "true" ]]; then
  echo "次: ./scripts/02-put-secrets.sh"
else
  echo "次: ./scripts/03-bootstrap-ec2.sh  （開発: app.env を配置）"
fi
echo "    ./scripts/05-schedule-ec2.sh"
echo "    SSH: ssh -i ${PEM_PATH} ec2-user@${PUBLIC_IP}"
