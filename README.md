# IT 部知识共享服务平台（it-docs）· 设计交付包

> English version: [README.en.md](README.en.md)
>
> 版本：v1.4（2026-11-20） · 容量口径：**约 1000 个注册读者、约 100 名内容维护者**（不是 1000 并发保证）
> 定位：替代旧静态知识库页（workbuddy.link），多人可编辑、实时生效、有权限和审计的内网知识平台

## 交付清单

| 文件 | 内容 | 给谁看 |
|---|---|---|
| `使用说明.md` / `USER-GUIDE.en.md` | 登录、检索、新建/编辑/发布、权限与常见问题 | 使用者 |
| `docs/需求文档.md` | 功能清单（一期 F1 / 二期 F2）、非功能需求、验收口径 | 评审用，先看这份 |
| `docs/架构文档.md` | 技术选型、容量设计、12 张表、API、权限链路、部署运维 | 系统工程师 / 开发 |
| `docs/效果图-新版/01~08-*.jpg` | **当前实现**的界面截图：首页 / 登录 / 工作台 / 列表 / 编辑 / 个人区域 / 深色 / 手机 | 汇报 / 走查 |
| `docs/原型图-界面.html` | 可交互原型：左侧目录点击跳屏，共 12 屏 | 评审用，浏览器直接打开（v1.3 设计阶段） |
| `docs/效果图/00~11-*.png` | v1.3 设计稿效果图（12 屏） | **历史版本**，界面已于 v1.4 改版，以 `效果图-新版` 为准 |
| `docs/效果图-api文档/` | Swagger UI 截图 | 接口调试参考 |
| `部署手册.md` / `DEPLOYMENT.en.md` | Linux + Docker 落地全流程、LDAP、备份、FAQ | 系统工程师 |

## 界面口径（v1.4）

- **查阅端只公开问题记录**：未登录首页只列 `qa`；业务文档、技术文档登录后在工作台看。
- **工作台按业务系统组织**：系统卡片 → 该系统的问题记录/业务文档/技术文档三个页签；左栏「工作台首页 / 全部问题记录 / 全部业务文档 / 全部技术文档 / 个人区域」。
- **登录页**：左侧品牌介绍 + 右侧表单（域账号 / 本地账号双通道）。
- **编辑器**：问题记录 / 业务文档 / 技术文档三种类型，培训手册用标签区分。

## 技术口径（一段话）

Java 8 + Spring Boot 2.7.18（单模块，包内分层 controller/service/repository/common）+ MySQL 8 + MyBatis-Plus，前端 Vue3 + Vite + Element Plus + md-editor-v3；认证 LDAP 域账号 + 本地账号双通道（JWT 会话）；文件元数据存 `it_file` 表，文件本体默认落本地 `./data/files`（local 存储），MinIO 为多副本可选实现；部署为单机 docker-compose（nginx + app×1 + mysql），不需要 MinIO / Redis。手机端查阅纯只读；公开内容免登录，需求与技术文档按空间及单篇授权控制。

## 页面一览（`docs/效果图-新版/`）

01 查阅端首页（免登录）→ 02 登录页 → 03 工作台首页（按业务系统）→ 04 问题记录列表 → 05 编辑页 → 06 个人区域 → 07 工作台·深色 → 08 工作台·手机

## 阅读顺序建议

1. `需求文档.md`（10 分钟，确认功能范围）
2. `效果图-新版/01~08-*.jpg`（3 分钟，确认界面）
3. `原型图-界面.html`（交互说明，设计阶段口径）
4. `架构文档.md`（评审技术与部署方案）

## 工程结构（已实施）

```
it-docs/
├── it-docs-api/              # Java 8 + Spring Boot 2.7.18 单模块（mvn package 已通过）
│   └── src/main/java/com/tyeeli/it/docs/
│       ├── ItDocsApplication.java #   启动类（放在根包，IDEA 里排最前）
│       ├── controller/       #   Web 层：9 个 Controller（只收参/校验/调 Service/转 DTO）
│       ├── dto/              #   请求/响应对象（Bean Validation；实体不出 Web 层）
│       ├── converter/        #   实体 ↔ DTO/DTO 转换
│       ├── config/           #   JWT 拦截器 / @RequireRole / 全局异常 / 首启初始化
│       ├── service/          #   认证 / 文档 / 空间权限 / 文件存储 / 审计
│       │   └── storage/      #   local（默认）+ MinIO（多副本可选，storage.type 切换）
│       ├── repository/              #   11 实体 + Mapper（MyBatis-Plus，无 XML）
│       └── common/           #   统一返回体 / 分页基类 / 异常 / 常量
├── it-docs-web/                     # Vue3 + Vite + Element Plus + md-editor-v3（npm build 已通过）
│   └── src/views/{portal,auth,console}/  # 查阅端（免登录）/ 登录 / 工作台+个人区域
├── sql/init.sql                # 12 张表 + ngram 全文索引 + 初始空间/字典
├── sql/upgrade-*.sql           # 老库升级脚本（全新安装不执行）
├── scripts/                    # deploy.sh / backup.sh / verify.sh
├── docker/                     # Dockerfile ×3（server/web/offline）+ nginx.conf
├── docker-compose.yml          # nginx+app×1+mysql（文件落 ./data/files）
├── kb-migrate.py               # 旧静态库 25 条卡 → 平台（已 dry-run 验证）
├── 使用说明.md / USER-GUIDE.en.md  # 使用者操作说明（中英）
├── 部署手册.md / DEPLOYMENT.en.md   # 内网落地全流程（中英，给系统工程师）
└── docs/                       # 需求/架构 v1.4 + 原型 + 效果图-新版（当前实现）
```

## 快速开始

```bash
# 开发机（详细步骤 + IDEA 断点调试见 本地调试.md）
双击 run-server.bat      # 后端 8080（用 Navicat 连的内网 MySQL，首次改 BAT 里账号密码）
双击 run-web.bat         # 前端 5173（vite 代理 /api，零跨域）
# 登录 admin / admin@123，LDAP 在「系统设置」页配置

# 服务器（见 部署手册.md）
cp .env.example .env && vi .env && bash scripts/deploy.sh
# 首启自动建 admin/admin@123（强制改密），浏览器开 http://<内网IP>/
```

## 接口文档（Swagger）

后端内置 springfox（Swagger 2），启动后端即可在线查看与调试：

| 入口 | 地址 | 用途 |
|---|---|---|
| 界面 | `http://localhost:8080/swagger-ui.html` | 在线调试：右上 Authorize 粘贴登录 token，即可调所有接口 |
| JSON | `http://localhost:8080/v2/api-docs` | 导入 Apifox / Postman / Postwoman |

- 9 个业务分组共 43 个操作；每个接口的参数、响应模型（`Result<T>` 泛型完整展开）、错误码说明都在页面里
- 公开接口（登录 / 文档列表详情 / 空间列表 / 附件预览下载）不带锁标记，可匿名试调
- 效果图见 `docs/效果图-api文档/01~03-*.png`

## 当前状态与下一步

- [x] 需求 / 架构（v1.4，约 1000 注册读者口径）、原型 + 效果图-新版（当前界面）
- [x] 后端工程（编译通过）：登录双通道、文档 CRUD+版本+发布、空间权限、文件 sha256 去重、审计
- [x] 前端工程（构建通过）：查阅端（免登录/移动适配）、登录、工作台、个人区域、Markdown+问答卡编辑
- [x] 界面 v1.4：查阅端只公开问题记录；工作台按业务系统组织；登录页左右分栏；浅色/深色 + 主色切换；内容分区迁移 `content_scope_v1`
- [x] 部署编排：compose + Dockerfile ×3 + nginx + .env 模板 + 部署/备份/校验脚本 + 部署手册
- [x] 接口文档：springfox + Swagger UI（43 个操作，JWT 调试就绪）
- [x] 迁移脚本 kb-migrate.py（旧库 25 条素材到位后执行）
- [ ] 服务器实机部署验证（Docker 在公司内网机，本机无 docker）
- [ ] LDAP 实连调通（管理台「系统设置」页配置 + 测试连接，或 .env 兜底）
- [ ] k6 压测（50 并发场景，交付前跑一次）
- [x] 域组授权（AD `memberOf`，需配置 LDAP 查询账号）
- [ ] 二期：钉钉机器人、通知推送
