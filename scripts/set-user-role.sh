#!/usr/bin/env bash
#
# Supabase Auth ユーザーの app_metadata.role を設定する（#90）。
#
# 使い方:
#   SUPABASE_URL=https://xxxx.supabase.co \
#   SUPABASE_SERVICE_ROLE_KEY=eyJ... \
#   ./scripts/set-user-role.sh <user-uuid> <general|operator|admin>
#
# - user-uuid はダッシュボードの Authentication > Users で確認できる
# - service_role キーは Admin API 用。絶対にコミット・クライアント配布しないこと
# - 反映はトークン再発行後（対象ユーザーの再ログイン / refresh）
set -euo pipefail

usage() {
  echo "usage: SUPABASE_URL=... SUPABASE_SERVICE_ROLE_KEY=... $0 <user-uuid> <general|operator|admin>" >&2
  exit 1
}

[[ $# -eq 2 ]] || usage

USER_ID="$1"
ROLE="$2"

case "$ROLE" in
  general | operator | admin) ;;
  *)
    echo "error: role must be one of: general, operator, admin (got: $ROLE)" >&2
    exit 1
    ;;
esac

if [[ ! "$USER_ID" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]; then
  echo "error: user-uuid must be a UUID (got: $USER_ID)" >&2
  exit 1
fi

: "${SUPABASE_URL:?SUPABASE_URL is required (e.g. https://xxxx.supabase.co)}"
: "${SUPABASE_SERVICE_ROLE_KEY:?SUPABASE_SERVICE_ROLE_KEY is required (Dashboard > Settings > API)}"

BASE_URL="${SUPABASE_URL%/}"

# GoTrue Admin API。app_metadata はトップレベルキー単位でマージされる（role だけ更新）。
curl -fsS -X PUT "$BASE_URL/auth/v1/admin/users/$USER_ID" \
  -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
  -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"app_metadata\":{\"role\":\"$ROLE\"}}"

echo
echo "done: user=$USER_ID role=$ROLE"
echo "note: 既存セッションの JWT には反映されない。対象ユーザーの再ログイン / refresh 後に有効。"
