# =====================================================================
# 前端镜像：node 构建 + nginx 托管（同时承担 /api 反代）
# 构建 context 为 it-docs/ 根目录：docker build -f docker/it-docs-web.Dockerfile .
# =====================================================================
FROM node:20-alpine AS builder
WORKDIR /build
ARG NPM_REGISTRY=https://registry.npmmirror.com
COPY it-docs-web/package*.json ./
RUN npm config set registry "$NPM_REGISTRY" && npm ci --no-audit --no-fund
COPY it-docs-web/ ./
RUN npm run build

FROM nginx:1.25-alpine
ENV LANG=C.UTF-8 TZ=Asia/Shanghai
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist/ /usr/share/nginx/html/
EXPOSE 80
