# ===== 阶段 1: 构建环境 =====
FROM ghcr.io/graalvm/graalvm-ce:ol8-java21-23.0.2 AS builder

# 安装 musl 开发工具和静态库
RUN microdnf install -y zip gzip gcc glibc-devel zlib-devel libstdc++-static \
    && microdnf clean all

# 下载并安装 musl libc
RUN curl -fsSL https://musl.libc.org/releases/musl-1.2.4.tar.gz | tar xz -C /tmp \
    && cd /tmp/musl-1.2.4 \
    && ./configure --prefix=/opt/musl \
    && make -j$(nproc) \
    && make install \
    && rm -rf /tmp/musl-1.2.4

# 设置工作目录
WORKDIR /build

# 复制 Maven 配置（利用缓存）
COPY pom.xml .

# 复制源代码
COPY src ./src

# 执行 Native 编译（使用 musl 进行完全静态链接）
# 通过命令行参数传递 --static 和 -H:UseMuslC=/opt/musl
RUN ./mvnw -B -Pnative native:compile -DskipTests \
    -DimageName=ai-interviewer \
    '-Dnative.additionalBuildArgs=--static,-H:UseMuslC=/opt/musl'

# 提取编译产物到输出目录
RUN mkdir -p /output && cp target/ai-interviewer /output/ai-interviewer

# ===== 阶段 2: 最小化运行时环境 =====
FROM scratch

# 从构建阶段复制静态二进制文件
COPY --from=builder /output/ai-interviewer /app/ai-interviewer

WORKDIR /app

ENTRYPOINT ["/app/ai-interviewer"]
