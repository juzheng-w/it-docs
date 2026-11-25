#!/usr/bin/env bash
# 首次部署 / 重启：校验 .env、构建镜像、启动并按 health 等待
set -euo pipefail

cd "$(dirname "$0")/.."
ROOT="$(pwd)"
ENV_FILE="$ROOT/.env"

if [ ! -f "$ENV_FILE" ]; then
  cat <<'EOF'
缺少 .env。请先手工创建（不要提交到代码库）：
  cp .env.example .env
  chmod 600 .env
  openssl rand -hex 24   # KB_DB_PASSWORD
  openssl rand -hex 48   # KB_JWT_SECRET
  vi .env
EOF
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

: "${KB_DB_PASSWORD:?KB_DB_PASSWORD 未设置}"
: "${KB_JWT_SECRET:?KB_JWT_SECRET 未设置}"

case "$KB_DB_PASSWORD" in
  root|ChangeMe*|change-me*)
    echo "KB_DB_PASSWORD 仍是默认/示例值，请改成随机值"; exit 1 ;;
esac
case "$KB_JWT_SECRET" in
  change-me-in-env|itdocs-dev-secret-change-me|ChangeMe*)
    echo "KB_JWT_SECRET 仍是默认/示例值，请改成随机值"; exit 1 ;;
esac

PORT="${KB_WEB_PORT:-80}"

command -v docker >/dev/null 2>&1 || { echo "未找到 docker"; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "未找到 docker compose v2"; exit 1; }

if command -v ss >/dev/null 2>&1; then
  if ss -lnt 2>/dev/null | awk 'NR>1 {print $4}' | grep -qE "[:.]${PORT}$"; then
    echo "端口 ${PORT} 已被占用，请改 KB_WEB_PORT 或释放端口"; exit 1
  fi
elif command -v netstat >/dev/null 2>&1; then
  if netstat -lnt 2>/dev/null | awk 'NR>2 {print $4}' | grep -qE "[:.]${PORT}$"; then
    echo "端口 ${PORT} 已被占用，请改 KB_WEB_PORT 或释放端口"; exit 1
  fi
fi

AVAIL_KB=$(df -Pk "$ROOT" | awk 'NR==2 {print $4}')
if [ -n "${AVAIL_KB:-}" ] && [ "$AVAIL_KB" -lt 10485760 ]; then
  echo "警告：$ROOT 剩余空间不足 10GB，首次构建可能失败"
fi

echo "==> 校验 compose 配置"
docker compose config >/dev/null

echo "==> 构建镜像（首次 10~20 分钟）"
docker compose build

echo "==> 启动"
docker compose up -d

echo "==> 等待健康检查"
HEALTH_URL="http://127.0.0.1:${PORT}/actuator/health"
OK=0
for _ in $(seq 1 60); do
  if command -v curl >/dev/null 2>&1; then
    if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then OK=1; break; fi
  else
    if wget -qO- "$HEALTH_URL" >/dev/null 2>&1; then OK=1; break; fi
  fi
  sleep 5
done

docker compose ps

if [ "$OK" != "1" ]; then
  echo "健康检查未通过：$HEALTH_URL"
  echo "--- app 日志 ---"
  docker compose logs --tail=100 app || true
  exit 1
fi

cat <<EOF
部署完成：http://<服务器IP>:${PORT}/
  管理员：admin / admin@123（首次登录强制改密）
  校验：bash scripts/verify.sh
  LDAP：admin 登录后 → 系统设置
EOF
