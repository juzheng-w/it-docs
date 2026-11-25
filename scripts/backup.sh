#!/usr/bin/env bash
# 每日备份：MySQL 全量 dump + 上传文件目录 tar；保留 14 天
set -euo pipefail
umask 077
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

cd "$(dirname "$0")/.."
TS=$(date +%F_%H%M%S)
mkdir -p data/backup

# 单实例锁（有 flock 才加，避免 cron 重入）
if command -v flock >/dev/null 2>&1; then
  exec 9>data/backup/.backup.lock
  flock -n 9 || { echo "已有备份在跑，跳过"; exit 0; }
fi

RUNNING=$(docker inspect -f '{{.State.Running}}' itdocs-mysql 2>/dev/null || true)
[ "$RUNNING" = "true" ] || { echo "itdocs-mysql 未运行"; exit 1; }

echo "==> 备份 MySQL"
docker exec itdocs-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --quick --routines --triggers --default-character-set=utf8mb4 it_docs' \
  | gzip > "data/backup/it_docs_${TS}.sql.gz"
gzip -t "data/backup/it_docs_${TS}.sql.gz"
[ -s "data/backup/it_docs_${TS}.sql.gz" ] || { echo "SQL 备份为空"; exit 1; }

if [ -d data/files ]; then
  echo "==> 备份上传文件"
  tar czf "data/backup/files_${TS}.tgz" -C data files
  tar -tzf "data/backup/files_${TS}.tgz" >/dev/null
fi

find data/backup -name 'it_docs_*.sql.gz' -mtime +14 -delete
find data/backup -name 'files_*.tgz' -mtime +14 -delete

echo "==> 备份完成"
ls -lh data/backup
