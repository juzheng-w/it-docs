# IT Department Knowledge Sharing Platform (it-docs)

> 中文版见 [README.md](README.md)
>
> Version: v1.4 (2026-11-20) · Capacity target: **~1,000 registered readers, ~100 content maintainers** (not a guarantee of 1,000 concurrent users)
> Purpose: replace the legacy static knowledge page with a multi-editor, real-time, permission-controlled and auditable internal knowledge platform

## What is in this repository

| File | Content | Audience |
|---|---|---|
| [使用说明.md](使用说明.md) / [USER-GUIDE.en.md](USER-GUIDE.en.md) | How to log in, search, create/edit and publish content | First-time users |
| [docs/需求文档.md](docs/需求文档.md) | Functional scope (phase 1 / phase 2), non-functional requirements, acceptance criteria | Reviewers — read this first |
| [docs/架构文档.md](docs/架构文档.md) | Technology stack, capacity design, 12 tables, APIs, permission flow, deployment | Engineers / developers |
| [部署手册.md](部署手册.md) / [DEPLOYMENT.en.md](DEPLOYMENT.en.md) | End-to-end Linux + Docker deployment, LDAP, backup, FAQ | System administrators |
| `docs/效果图-新版/01~08-*.jpg` | Screenshots of the **current** UI: home / login / console / list / editor / personal area / dark mode / mobile | Presentations, walkthroughs |
| `docs/原型图-界面.html` | Interactive prototype, 12 screens (v1.3 design phase) | Review |
| `docs/效果图/00~11-*.png` | v1.3 design mockups — **historical**, superseded by `效果图-新版` | Reference only |
| `docs/效果图-api文档/` | Swagger UI screenshots | API debugging reference |

## UI model (v1.4)

- **The public portal only exposes Q&A records**: anonymous visitors see `qa` content only. Business documents and technical documents are visible in the console after login.
- **The console is organized by business system**: system cards → three tabs (Q&A / business docs / technical docs) for each system. The left menu holds "Console home / All Q&A / All business docs / All technical docs / Personal area".
- **Login page**: brand panel on the left, form on the right (domain account or local account).
- **Editor**: three content types (Q&A, business document, technical document); training manuals are distinguished by tags.

## Technology (one paragraph)

Java 8 + Spring Boot 2.7.18 (single module, layered packages controller/service/repository/common) + MySQL 8 + MyBatis-Plus; front end Vue 3 + Vite + Element Plus + md-editor-v3. Authentication supports LDAP domain accounts and local accounts (JWT sessions). File metadata lives in `it_file`; file bodies are stored on local disk under `./data/files` by default (local storage), with MinIO available as an optional multi-replica backend. Deployment is a single-host docker-compose stack (nginx + one app instance + MySQL); no MinIO or Redis is required.

## Screens (`docs/效果图-新版/`)

01 public portal (anonymous) → 02 login → 03 console home (by business system) → 04 Q&A list → 05 editor → 06 personal area → 07 console dark mode → 08 console mobile

## Suggested reading order

1. `docs/需求文档.md` (10 minutes — confirm the functional scope)
2. `docs/效果图-新版/01~08-*.jpg` (3 minutes — confirm the UI)
3. `docs/原型图-界面.html` (interaction notes, design-phase wording)
4. `docs/架构文档.md` (technology and deployment review)

## Project layout

```
it-docs/
├── it-docs-api/              # Java 8 + Spring Boot 2.7.18 single module (mvn package passes)
│   └── src/main/java/com/tyeeli/it/docs/
│       ├── ItDocsApplication.java #   entry point
│       ├── controller/       #   Web layer: 9 controllers (params, validation, service call, DTO)
│       ├── dto/              #   request/response models (Bean Validation; entities never leave the service layer)
│       ├── converter/        #   entity ↔ DTO converters
│       ├── config/           #   JWT interceptor / @RequireRole / global exception handler / first-run initializer
│       ├── service/          #   auth / document / space permission / file storage / audit
│       │   └── storage/      #   local (default) + MinIO (optional multi-replica backend)
│       ├── repository/       #   entities + MyBatis-Plus mappers (no XML)
│       └── common/           #   unified response body / paging base class / exceptions / constants
├── it-docs-web/                     # Vue 3 + Vite + Element Plus + md-editor-v3 (npm build passes)
│   └── src/views/{portal,auth,console}/  # public portal / login / console + personal area
├── sql/init.sql                # 12 tables + ngram full-text indexes + seed spaces and dictionaries
├── sql/upgrade-*.sql           # upgrade scripts for existing databases (not used on a fresh install)
├── scripts/                    # deploy.sh / backup.sh / verify.sh
├── docker/                     # 3 Dockerfiles (app / web / offline) + nginx.conf
├── docker-compose.yml          # nginx + app ×1 + MySQL (uploaded files go to ./data/files)
├── kb-migrate.py               # legacy static knowledge base migration (dry-run verified)
├── 使用说明.md / USER-GUIDE.en.md   # end-user guide
├── 部署手册.md / DEPLOYMENT.en.md    # deployment manual
└── docs/                       # requirements / architecture v1.4 + prototype + current screenshots
```

## Quick start

```bash
# Developer machine (see 本地调试.md for the detailed, IDEA-friendly walkthrough)
run-server.bat           # backend on :8080 (uses the MySQL you point it at)
run-web.bat              # frontend on :5173 (Vite proxies /api, no CORS setup)
# Log in as admin / admin@123; configure LDAP under "System settings"

# Server (see 部署手册.md or DEPLOYMENT.en.md)
cp .env.example .env && vi .env && bash scripts/deploy.sh
# admin / admin@123 is created on first start (password change enforced);
# open http://<server-ip>/
```

## API documentation (Swagger)

The backend ships springfox (Swagger 2). Start the backend and browse:

| Entry | URL | Purpose |
|---|---|---|
| UI | `http://localhost:8080/swagger-ui.html` | Try calls: click Authorize, paste the login token, then call any endpoint |
| JSON | `http://localhost:8080/v2/api-docs` | Import into Apifox / Postman / Postwoman |

- 9 business groups, 43 operations; parameters, response models (`Result<T>` fully expanded) and error codes are documented in the UI
- Public endpoints (login, document list/detail, space list, attachment preview/download) carry no padlock and can be called anonymously

## Status and next steps

- [x] Requirements / architecture (v1.4), prototype, current screenshots
- [x] Backend: dual login channels, document CRUD + versions + publishing, space permissions, sha256 file de-duplication, audit log
- [x] Frontend: public portal (anonymous, mobile-friendly), login, console, personal area, Markdown + Q&A card editor
- [x] UI v1.4: public portal exposes Q&A only; console grouped by business system; split login page; light/dark theme + accent color; `content_scope_v1` data migration
- [x] Deployment: compose + 3 Dockerfiles + nginx + `.env` template + deploy/backup/verify scripts + deployment manual
- [x] API documentation: springfox + Swagger UI (43 operations, JWT-ready)
- [x] User guide (Chinese and English)
- [x] Migration script `kb-migrate.py` (run once the 25 legacy cards are ready)
- [ ] On-server verification (Docker runs on the intranet host; the dev machine has no Docker)
- [ ] LDAP end-to-end test against the production domain controller
- [ ] k6 load test (50 concurrent scenario) before go-live
- [x] Domain-group authorization (AD `memberOf`, requires a bind account)
- [ ] Phase 2: DingTalk bot, push notifications
