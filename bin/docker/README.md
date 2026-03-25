# GraalVM Native Image 静态编译指南

本项目使用 GraalVM Native Image 配合 musl libc 实现完全静态编译，生成不依赖任何动态库的独立二进制文件。

## 目录结构

```
bin/
└── docker/
    ├── Dockerfile          # 构建镜像配置
    ├── build.sh            # 自动化构建脚本
    └── README.md           # 本说明文件
```

## 快速开始

### 方法一：使用自动化脚本（推荐）

```bash
# 执行构建脚本
./bin/docker/build.sh
```

构建完成后，二进制文件将位于 `bin/ai-interviewer`

### 方法二：手动执行

#### 1. 构建 Docker 镜像

```bash
cd /path/to/project
docker build -f bin/docker/Dockerfile -t ai-interviewer-builder --target builder .
```

#### 2. 提取编译产物

```bash
# 创建临时容器
CONTAINER_ID=$(docker create ai-interviewer-builder)

# 复制二进制文件到本地
docker cp $CONTAINER_ID:/output/ai-interviewer ./bin/ai-interviewer

# 清理容器
docker rm $CONTAINER_ID
```

#### 3. 验证产物

```bash
# 检查文件类型（应显示 statically linked）
file bin/ai-interviewer

# 检查动态库依赖（应显示 not a dynamic executable 或 statically linked）
ldd bin/ai-interviewer
```

## 技术细节

### 为什么使用 musl libc？

- **完全静态链接**：musl libc 允许完全静态链接，生成的二进制文件不依赖系统的 glibc
- **跨平台兼容**：静态链接的二进制文件可以在任何相同架构的 Linux 系统上运行
- **最小化部署**：可以放入 scratch 容器，无需任何基础镜像

### Dockerfile 说明

1. **阶段 1 (builder)**:
   - 使用 GraalVM CE 23.0.2 (Java 21) 作为基础镜像
   - 安装 musl libc 并编译
   - 执行 Maven Native Image 编译，传递 `--static` 和 `-H:UseMuslC=/opt/musl` 参数
   - 输出静态链接的二进制文件到 `/output`

2. **阶段 2 (runtime)**:
   - 使用 `scratch` (空镜像) 作为基础
   - 仅包含编译后的二进制文件
   - 最终镜像大小仅包含二进制文件本身

### pom.xml 配置要点

```xml
<profile>
    <id>native</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <buildArgs>
                        <!-- 完全静态编译 -->
                        <buildArg>--static</buildArg>
                        <buildArg>-H:UseMuslC=/opt/musl</buildArg>
                        
                        <!-- JNI 支持 (DuckDB 必需) -->
                        <buildArg>--enable-jni</buildArg>
                        
                        <!-- 资源包含 -->
                        <buildArg>-H:IncludeResources=.*\.onnx$</buildArg>
                        <buildArg>-H:IncludeResources=.*\.db$</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### 关键参数说明

| 参数 | 说明 |
|------|------|
| `--static` | 启用完全静态链接 |
| `-H:UseMuslC=/opt/musl` | 指定 musl libc 路径 |
| `--enable-jni` | 启用 JNI 支持（DuckDB 必需） |
| `-H:+JNI` | 额外 JNI 配置 |
| `-H:IncludeResources=.*\.onnx$` | 包含 ONNX 模型文件 |
| `-H:IncludeResources=.*\.db$` | 包含数据库文件 |

## 常见问题

### Q: 构建速度慢怎么办？

A: Native Image 编译本身较慢，首次构建可能需要 10-20 分钟。可以利用 Docker 层缓存加速后续构建。

### Q: 出现 "failed to find libm.so" 错误？

A: 确保使用了 `--static` 参数和 musl libc。检查 Dockerfile 中 musl 是否正确安装。

### Q: 二进制文件太大？

A: 可以尝试以下优化：
- 启用压缩：添加 `-O2` 或 `-O3` 优化级别
- 移除调试信息：添加 `--no-debug-info`
- 使用 UPX 压缩（可选）

### Q: 运行时出现权限错误？

A: 确保二进制文件有执行权限：
```bash
chmod +x bin/ai-interviewer
```

### Q: 如何在其他 Linux 发行版上运行？

A: 由于使用了 musl libc 进行完全静态链接，生成的二进制文件可以在任何 x86_64 Linux 系统上运行，无需安装任何依赖。

## 参考链接

- [GraalVM Native Image](https://www.graalvm.org/latest/docs/native-image/)
- [musl libc](https://musl.libc.org/)
- [native-maven-plugin](https://github.com/graalvm/native-maven-plugin)
