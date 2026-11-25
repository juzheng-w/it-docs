# =====================================================================
# 后端镜像：多阶段构建（单模块 it-docs-api）
# 阶段1 maven 构建；阶段2 只含 JRE8 的运行镜像
# =====================================================================
FROM maven:3.8-eclipse-temurin-8 AS builder
WORKDIR /build
COPY it-docs-api/ ./it-docs-api/
RUN cd it-docs-api && mvn -B -q package -DskipTests

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=builder /build/it-docs-api/target/it-docs.jar ./app.jar
ENV LANG=C.UTF-8 TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
EXPOSE 8080
# exec 让 java 成为 PID 1，docker stop 时能收到 SIGTERM 优雅退出
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
