# 离线构建镜像：服务器无外网时用（产物由开发机构建后拷入）
# 前置：it-docs-api/target/it-docs.jar 已存在
# 构建命令（context 为 it-docs 根目录）：
#   docker build -f docker/offline-app.Dockerfile -t itdocs-app .
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY it-docs-api/target/it-docs.jar ./app.jar
ENV LANG=C.UTF-8 TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
