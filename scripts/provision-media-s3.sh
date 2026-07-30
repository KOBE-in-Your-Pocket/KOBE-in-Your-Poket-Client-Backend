#!/usr/bin/env bash
#
# 画像アップロード用の S3 / IAM をプロビジョニングする（#86 / media-upload-s3）。
#
# 冪等（再実行可）。AWS 管理者権限で実行する。作成/更新するもの:
#   1. S3 バケット（未作成なら作成）
#   2. uploads/* の公開読み取りバケットポリシー（PUBLIC_READ=true のとき）
#   3. Backend が動く EC2 実行ロールへ s3:PutObject（インラインポリシー）
#
# 認証情報はここには書かない。Backend は AWS SDK 既定チェーン（EC2 の IAM ロール）で認証する。
#
# 使い方（AWS 認証済みで実行）:
#   ./scripts/provision-media-s3.sh
#
# 変数（環境変数で上書き可）:
#   BUCKET       (default: kobe-in-your-pocket-media)
#   REGION       (default: ap-northeast-1)
#   EC2_ROLE     (default: kobe-backend-ec2-role)   … Backend EC2 のインスタンスロール
#   KEY_PREFIX   (default: uploads)                 … backend の UploadMediaService と一致
#   PUBLIC_READ  (default: true)                    … CloudFront を使うなら false
set -euo pipefail

BUCKET="${BUCKET:-kobe-in-your-pocket-media}"
REGION="${REGION:-ap-northeast-1}"
EC2_ROLE="${EC2_ROLE:-kobe-backend-ec2-role}"
KEY_PREFIX="${KEY_PREFIX:-uploads}"
PUBLIC_READ="${PUBLIC_READ:-true}"
POLICY_NAME="kobe-backend-media-s3-putobject"

command -v aws >/dev/null || {
  echo "error: aws CLI が必要です" >&2
  exit 1
}

ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
echo "AWS account=${ACCOUNT} region=${REGION} bucket=${BUCKET} role=${EC2_ROLE} prefix=${KEY_PREFIX}/"

# 1. バケット作成（冪等）
if aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  echo "[1/3] bucket exists: ${BUCKET}"
else
  echo "[1/3] creating bucket: ${BUCKET}"
  if [[ "$REGION" == "us-east-1" ]]; then
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" >/dev/null
  else
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" \
      --create-bucket-configuration "LocationConstraint=${REGION}" >/dev/null
  fi
fi

# 2. 公開読み取り（uploads/* のみ）
if [[ "$PUBLIC_READ" == "true" ]]; then
  echo "[2/3] applying public-read bucket policy for ${KEY_PREFIX}/*"
  # バケットポリシーを効かせるため、ポリシー系のブロックのみ解除（ACL は使わない）
  aws s3api put-public-access-block --bucket "$BUCKET" \
    --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=false,RestrictPublicBuckets=false" >/dev/null
  aws s3api put-bucket-policy --bucket "$BUCKET" --policy "$(
    cat <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadUploads",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${BUCKET}/${KEY_PREFIX}/*"
    }
  ]
}
JSON
  )"
else
  echo "[2/3] PUBLIC_READ=false: バケットポリシーは適用しない（CloudFront 前提。MEDIA_S3_PUBLIC_BASE_URL を設定）"
fi

# 3. EC2 実行ロールへ s3:PutObject を付与（インラインポリシー・冪等）
echo "[3/3] granting s3:PutObject to role ${EC2_ROLE} (${POLICY_NAME})"
aws iam put-role-policy --role-name "$EC2_ROLE" --policy-name "$POLICY_NAME" \
  --policy-document "$(
    cat <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PutMediaUploads",
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::${BUCKET}/${KEY_PREFIX}/*"
    }
  ]
}
JSON
  )"

cat <<EOF

done. Backend の env に以下を設定してください（EC2 の env / CD 設定。docs/infrastructure/ec2-app-env.md 参照）:
  MEDIA_S3_BUCKET=${BUCKET}
  MEDIA_S3_REGION=${REGION}
  # CloudFront を使う場合のみ（空なら https://${BUCKET}.s3.${REGION}.amazonaws.com/<key> を返す）:
  # MEDIA_S3_PUBLIC_BASE_URL=https://xxxx.cloudfront.net
EOF
