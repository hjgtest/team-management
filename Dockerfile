# ==========================================
# 开发组管理系统 —— 云端部署镜像
# 多阶段构建：Maven 编译打包 → 仅用 JRE 8 运行，镜像体积可控
# 前端构建产物已随仓库提交在 src/main/resources/static，无需在容器内装 Node
# ==========================================

# ---------- 构建阶段 ----------
FROM maven:3.8-jdk-8 AS builder
WORKDIR /build

# 先单独拷贝 pom，利用 Docker 层缓存，改代码时不必重新下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:8-jre

ENV TZ=Asia/Shanghai LANG=C.UTF-8

# 免费实例内存只有 512MB：70% 给堆，SerialGC 省内存，线程栈压到 512k
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

WORKDIR /app
COPY --from=builder /build/target/team-management-system-0.0.1-SNAPSHOT.jar app.jar

# 平台会注入 PORT 环境变量覆盖此默认值
EXPOSE 8080

# 用 sh -c 让 JAVA_OPTS 能被展开
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
