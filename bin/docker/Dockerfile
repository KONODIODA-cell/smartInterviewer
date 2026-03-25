# ===== 阶段1: 构建环境 =====
FROM ghcr.io/graalvm/graalvm-ce:ol8-java21-23.0.2 AS builder

# 安装构建依赖
RUN microdnf install -y zip gzip gcc glibc-devel zlib-devel libstdc++-static \
    && microdnf clean all

# 设置工作目录
WORKDIR /build

# 复制 Maven 配置（利用缓存）
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# 下载依赖（不编译）
RUN ./mvnw -B dependency:go-offline || true

# 复制源代码
COPY src ./src

# 执行 Native 构建
RUN ./mvnw -B -Pnative native:compile -DskipTests \
    -DimageName=ai-interviewer

# 提取编译产物到项目目录下
