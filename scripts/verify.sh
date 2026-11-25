#!/usr/bin/env bash
# 部署后校验：health + compose 状态 + 表/种子/管理员 + 公开字典接口
set -euo pipefail

cd "$(dirname "$0")/.."
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
PORT="${KB_WEB_PORT:-80}"
HEALTH_URL="http://127.0.0.1:${PORT}/actuator/health"

echo "==> health"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "$HEALTH_URL"; echo
else
  wget -qO- "$HEALTH_URL"; echo
fi

echo "==> compose ps"
docker compose ps

echo "==> schema / seed / admin"
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot it_docs' \
  < sql/verify-schema.sql

echo "==> public meta api"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://127.0.0.1:${PORT}/api/meta" >/dev/null && echo "OK /api/meta"
else
  wget -qO- "http://127.0.0.1:${PORT}/api/meta" >/dev/null && echo "OK /api/meta"
fi
