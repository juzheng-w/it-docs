# it-docs Deployment Guide (single Linux host · Docker)

> 中文版见 [部署手册.md](部署手册.md)
>
> Topology: nginx (web) + Spring Boot (app ×1) + MySQL 8. Uploaded files are stored on local disk under `./data/files` — **no MinIO and no Redis required**.
> Audience: system administrators. If something goes wrong, start with section 11 (FAQ).

## 1. Prerequisites

| Item | Requirement |
|---|---|
| Server | Linux x86_64, 4 vCPU / 8 GB RAM to start (4/4 works too), system disk ≥ 50 GB |
| Docker | 20.10+, and `docker compose version` must print a version (Compose v2) |
| Network | The server must reach the internet (image pulls, Maven, npm); domain login also needs access to the LDAP domain controller |
| Ports | Only the web port is exposed (`KB_WEB_PORT`, default 80); MySQL 3306 and app 8080 stay inside the container network |
| System | Time zone `Asia/Shanghai` recommended; if SELinux is Enforcing see section 11 |

Only one web port is exposed. All business data lives under `./data/` in the project directory:

```
/opt/it-docs/
├── docker-compose.yml
├── .env                    # created by hand, mode 600, never committed
├── sql/                    # init.sql + upgrade scripts + verification script
├── scripts/                # deploy.sh / backup.sh / verify.sh
└── data/
    ├── mysql/              # MySQL data directory
    ├── files/              # uploaded images and attachments (local storage)
    └── backup/             # backup output
```

`data/` must be kept and backed up. Deleting `data/mysql` is equivalent to wiping the database.

## 2. First deployment

### 2.1 Pre-flight checks

```bash
docker --version
docker compose version
df -h /opt
free -h
ss -lntp | grep ':80 ' || true
getenforce 2>/dev/null || true
```

If Docker is not running, run `systemctl enable --now docker` so containers come back after a reboot.

### 2.2 Put the code in `/opt/it-docs`

scp, rsync or tar all work. Do not copy your development machine's `node_modules`, `target` or `data` directories; the root `.dockerignore` already excludes them from the build context.

### 2.3 Create `.env` by hand (required)

`.env` contains passwords — **never commit it and never share it**. The repository only ships `.env.example`:

```bash
cd /opt/it-docs
cp .env.example .env
chmod 600 .env

# generate random values and paste them into .env
openssl rand -hex 24   # KB_DB_PASSWORD
openssl rand -hex 48   # KB_JWT_SECRET

vi .env
```

Required values:

| Variable | Notes |
|---|---|
| `KB_DB_PASSWORD` | MySQL root password; **do not use the example value**. Once the database is initialised, changing it means changing the database password too |
| `KB_JWT_SECRET` | JWT signing key, ≥ 32 random characters; leaking it lets anyone forge a login |
| `KB_WEB_PORT` | Exposed web port, default 80; use 8081 etc. if taken |
| `KB_JAVA_OPTS` | Optional, defaults to `-Xms512m -Xmx1024m -Dfile.encoding=UTF-8`; lower it on small machines |

LDAP can stay empty and be configured in the admin UI after deployment (section 4).
`KB_MINIO_ACCESS_KEY` / `KB_MINIO_SECRET_KEY` in `.env.example` are legacy entries — **not read** in local storage mode; leave them empty.

### 2.4 Build and start

```bash
cd /opt/it-docs
bash scripts/deploy.sh
```

The script checks `.env` / Docker / port / disk, runs `docker compose config`, builds the images (10–20 minutes the first time), starts the stack, then polls `http://127.0.0.1:${KB_WEB_PORT}/actuator/health`. When the health check passes it prints the URL.

Equivalent manual commands:

```bash
docker compose config
docker compose build
docker compose up -d
docker compose ps
```

### 2.5 Where the database and initial account come from

- On first start `data/mysql` is empty, so MySQL's `docker-entrypoint-initdb.d` executes `sql/init.sql`: it creates the `it_docs` database, 12 tables, 3 spaces and two seed dictionaries.
- Once MySQL is healthy the app starts; `DataInitializer` idempotently adds any missing columns/tables and creates the local administrator `admin / admin@123` (password change enforced on first login) if `it_user` has no `admin`.
- `sql/init.sql` runs **only once, when the MySQL data directory is empty**. Editing it later has no effect; do not re-run the schema script against an initialised database.
- For a fresh install do **not** run `sql/upgrade-*.sql`; those are for upgrading existing databases.

### 2.6 Post-deployment verification

```bash
cd /opt/it-docs
bash scripts/verify.sh
```

Expected output:

- `{"status":"UP"}`
- mysql / app / web all `healthy` in `docker compose ps`
- `table_count = 12`
- `it_space` contains `QA` (PUBLIC), `REQ` (INTERNAL), `TECH` (INTERNAL)
- `it_meta` has 5 `BIZ_SYSTEM` and 5 `CATEGORY` rows
- `it_user` contains `admin` with `must_change_pwd = 1`
- `/api/meta` is reachable anonymously

Open `http://<server-ip>:<KB_WEB_PORT>/` → sign in with `admin / admin@123` → change the password when prompted.

## 3. Built-in seed data

Data present after the first start (from `sql/init.sql`):

| Type | Content |
|---|---|
| Spaces | `QA` Q&A (PUBLIC, anonymous); `REQ` business documents (INTERNAL); `TECH` technical documents (INTERNAL) |
| Business systems | SVS, OTWB, MES, DingTalk, Email (5 rows) |
| Issue categories | Account & permissions, API errors, Report issues, Data fixes, Environment setup (5 rows) |

After go-live the administrator maintains these under "Console → Base data"; categories can be bound to a business system via `parent_id`.
To use your own dictionaries **before the first start**, edit the seed `INSERT` statements in `sql/init.sql`. Once `data/mysql` exists, `init.sql` will not run again — use the admin UI or manual SQL instead.

## 4. LDAP integration

Configure it in the admin UI: sign in as admin → "System settings" → fill in → **Test connection** → Save. Changes take effect immediately, no container restart. The configuration is stored in `it_config` and takes precedence over `.env`.

There are six values:

| Field | AD example | OpenLDAP example | Notes |
|---|---|---|---|
| LDAP URL | `ldap://ad.corp.com:389` | `ldap://ldap.corp.com:389` | Use `ldaps://...:636` for LDAPS |
| Base DN | `DC=corp,DC=com` | `dc=corp,dc=com` | Domain / directory root |
| User DN pattern | leave empty | `uid={0},ou=people,DC=corp,DC=com` | `{0}` is replaced with the login name |
| UPN pattern | `{0}@corp.com` | leave empty | AD uses this; both templates are tried in order |
| Bind account | `svc-itdocs@corp.com` | `cn=readonly,dc=corp,dc=com` | Only used for "Import from AD" and domain-group authorisation; not needed for login |
| Bind password | corresponding password | corresponding password | Same as above |

Login names:

- AD: use `sAMAccountName` (e.g. `zhangsan`), without the `CORP\` prefix.
- OpenLDAP: use `uid`.
- A domain user is **provisioned on first login** with role `VIEWER`; the administrator raises it to EDITOR/ADMIN under "Members" and can set `sys_scope` (visible business systems; empty = all).
- If LDAP fails, the local `admin` account still works as a break-glass channel.
- "Import from AD" requires the bind account; imported accounts have no local password and still authenticate with the domain password.
- Domain-group authorisation (`LDAP_GROUP`) is implemented: it matches the AD `memberOf` attribute, and ACL entries may use the group `CN` or the full `DN`, case-insensitively. Only direct memberships are matched (no nested groups), and results are cached for 5 minutes. **It requires the bind account**; without it, group ACLs never match, though direct user grants still work.
- LDAPS on 636: if your CA is self-signed, import it into the container's JRE truststore and point `KB_JAVA_OPTS` at the truststore; otherwise use 389 on the intranet first.

## 5. Day-2 operations

| Task | Command |
|---|---|
| Status | `docker compose ps` |
| Health | `curl http://127.0.0.1:${KB_WEB_PORT}/actuator/health` |
| Logs | `docker compose logs -f app` / `mysql` / `web` |
| Restart app | `docker compose restart app` |
| Stop / start | `docker compose down` / `docker compose up -d` (data in `./data/` is preserved) |
| Disk usage | `du -sh data/*` |

Container logs rotate automatically (`json-file`, 10 MB per file, 3 files kept).

### Releases and rollback

Back up first, and tag the current images for rollback:

```bash
cd /opt/it-docs
docker tag itdocs-app:latest itdocs-app:backup-$(date +%F)
docker tag itdocs-web:latest itdocs-web:backup-$(date +%F)

bash scripts/backup.sh
docker compose build app web
docker compose up -d
bash scripts/verify.sh
```

Roll the images back if something is wrong (data untouched):

```bash
docker tag itdocs-app:backup-2026-11-24 itdocs-app:latest
docker tag itdocs-web:backup-2026-11-24 itdocs-web:latest
docker compose up -d --no-build
```

## 6. Backup and restore

### Daily backup

```bash
cd /opt/it-docs
bash scripts/backup.sh
```

The script will:

1. dump `it_docs` with `mysqldump --single-transaction`, gzipped to `data/backup/it_docs_<timestamp>.sql.gz`;
2. archive `data/files` into `data/backup/files_<timestamp>.tgz`;
3. verify the archives are readable and non-empty;
4. delete backups older than 14 days.

Cron example (`/etc/cron.d/itdocs-backup`):

```cron
30 2 * * * root /bin/bash /opt/it-docs/scripts/backup.sh >> /var/log/itdocs-backup.log 2>&1
```

Also sync `data/backup` to another machine or a NAS — a single-host backup cannot survive disk failure.

### Restore

Stop app/web first so nothing writes during the restore:

```bash
cd /opt/it-docs
docker compose stop app web

# 1) restore the database (database it_docs; the MySQL container keeps running)
gunzip -c data/backup/it_docs_2026-11-24_023000.sql.gz \
  | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot it_docs'

# 2) restore uploaded files
tar xzf data/backup/files_2026-11-24_023000.tgz -C data

docker compose start app web
bash scripts/verify.sh
```

If the MySQL data directory itself is damaged, first bring up an empty database (`docker compose down`, move `data/mysql` aside, `docker compose up -d mysql` and wait for `init.sql` to create the schema), then restore the dump as above. **Do not delete `data/mysql` before confirming that a usable backup exists.**

## 7. Upgrading an existing database

Only needed when upgrading from an older version; skip this section for a fresh install.

Run `bash scripts/backup.sh` first. The scripts below are safe to re-run (they inspect `information_schema` themselves):

| Script | Purpose | When |
|---|---|---|
| `sql/upgrade-doc-acl.sql` | Per-document access table | When upgrading to the version with per-document ACLs |
| `sql/upgrade-2026-11-18-search-summary.sql` | Adds `summary` to the full-text index | Upgrading to v1.3 |
| `sql/upgrade-2026-11-20-auth-hardening.sql` | `token_version`, last-login timestamps | Upgrading to the hardened authentication version |
| `sql/upgrade-2026-11-20-content-scope.sql` | Content scoping (REQ space, tighter visibility) | Upgrading to "portal exposes Q&A only" |
| `sql/upgrade-2026-11-23-version-snapshot.sql` | Full-field version snapshots (rollback restores summary/system/category etc.) | Upgrading to the full rollback version |

Run them from the project root:

```bash
docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot it_docs' \
  < sql/upgrade-2026-11-23-version-snapshot.sql
```

`DataInitializer` also applies the same changes idempotently at startup; the scripts are mainly for "database first, release later" or manual verification.

## 8. Offline / restricted networks

When the server has internet access, section 2 is all you need. If it cannot reach Docker Hub / Maven / npm, build the images on a machine that can and copy them over:

```bash
# on a machine with internet, in the project root
docker compose build
docker save itdocs-app:latest itdocs-web:latest -o itdocs-images.tar
# also prepare mysql:8.0
docker pull mysql:8.0
docker save mysql:8.0 -o mysql-8.0.tar

# on the server
docker load -i itdocs-images.tar
docker load -i mysql-8.0.tar
docker compose up -d --no-build
```

If you only have the app artifact and no Docker build environment, build with `docker/offline-app.Dockerfile` after copying `it-docs-api/target/it-docs.jar` in place; for the front end, put `it-docs-web/dist/` into an nginx image. None of this is needed for an online deployment.

## 9. Capacity and scaling limits

- The current deployment is **single host, single replica**: local file storage + one app instance. `docker compose up --scale app=2` is not production-ready because the replicas cannot see each other's local upload directory.
- To scale out, first move storage to object storage (`KB_STORAGE_TYPE=minio`, requires a separate MinIO/S3 deployment) and point every app replica at the same MySQL. In-app Caffeine caches are per instance, so permission and dictionary changes may be inconsistent across replicas for seconds to minutes; accept that or refactor later.
- **Redis is not wired in.** Starting a Redis container does not enable any caching.
- Consider Elasticsearch only when the document count reaches tens of thousands and search slows down; MySQL FULLTEXT with ngram is sufficient for phase one.

## 10. Go-live acceptance checklist

1. `docker compose ps`: mysql / app / web all `healthy`.
2. `docker compose logs app` contains `Started ItDocsApplication`.
3. `curl http://127.0.0.1:${KB_WEB_PORT}/actuator/health` → `{"status":"UP"}`.
4. `bash scripts/verify.sh`: 12 tables, 3 spaces, 5 rows in each dictionary, `admin` present.
5. Browser: the portal opens anonymously; admin signs in → forced password change → signs in again.
6. Create a Q&A record (system / category / one-line answer / body) → save → it appears on the portal.
7. Upload an image → still there after refresh (file under `data/files`).
8. Create a business document → invisible after signing out / in a private window; visible after login.
9. Edit one document twice → two versions → roll back to the older one → title / summary / system / category / owner role / tags / body are restored.
10. LDAP: test connection succeeds; a real domain account signs in and is provisioned; after promotion it can edit.
11. Persistence: documents, images and accounts survive `docker compose restart`.
12. Backup: `bash scripts/backup.sh` produces a non-empty `.sql.gz` and `files_*.tgz`; rehearse a restore against a test database.
13. After rebooting the server, `docker compose ps` shows everything running again.

## 11. FAQ

| Problem | Resolution |
|---|---|
| app keeps restarting | Check `docker compose logs app`; common causes are MySQL not initialised yet, `KB_DB_PASSWORD` not matching the password in the existing data directory, or an empty `KB_JWT_SECRET` |
| MySQL never becomes healthy | `docker compose logs mysql`; first-time initialisation takes 30–60 s; verify `sql/init.sql` has no syntax error — the health check queries `it_docs.it_space` |
| Port 80 is taken | Set `KB_WEB_PORT=8081` in `.env` and recreate web with `docker compose up -d` |
| 502 Bad Gateway | Check `docker compose ps` for app health and `docker compose logs app`; nginx resolves app through Docker DNS, so web does not need a restart after app is rebuilt |
| Uploaded images 404 | Check `docker compose logs app`; make sure `./data/files` exists and is writable; no MinIO is involved in local mode |
| "Domain authentication service unavailable" | Wrong LDAP URL / Base DN, or the domain controller is unreachable; on the server try `nslookup ad.corp.com`, `curl -v telnet://ad.corp.com:389`; use the local admin account as a fallback |
| LDAP test succeeds but login fails | AD usually needs the UPN pattern `{0}@corp.com` and a login name without `CORP\`; OpenLDAP needs a DN pattern such as `uid={0},...`; make sure the user is searchable under the Base DN |
| Domain user cannot see business documents | First login provisions `VIEWER`; promote the account under "Members" and check `sys_scope` |
| Domain-group authorisation has no effect | Confirm the bind account and password under "System settings"; `LDAP_GROUP` only supports AD `memberOf`, no nested groups; results are cached for 5 minutes |
| Changed `.env` but nothing happened | Environment variables are read when the container is created: use `docker compose up -d` (recreate), not `restart` |
| SELinux blocks the bind mounts | With `getenforce` Enforcing, append `:Z` to the `./data` and `./sql` volumes in compose, or run `chcon -Rt container_file_t data sql` |
| Lost the admin password | Another admin can reset it under "Members"; if it is the only admin, restore from backup, or have a DBA generate a BCrypt hash and update `it_user.password_hash` together with `must_change_pwd=1, token_version=token_version+1` |
| Start completely over | Confirm a backup exists first; `docker compose down`, delete `data/mysql` (and `data/files` if desired), then `up -d` — `init.sql` re-runs and admin is recreated. **This cannot be undone** |
