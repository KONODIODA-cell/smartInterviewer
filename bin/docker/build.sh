#!/bin/bash
set -e

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "========================================="
echo "GraalVM Native Image 静态编译脚本"
echo "========================================="
echo ""

# 进入项目根目录
cd "$PROJECT_ROOT"

# 构建 Docker 镜像（只构建到 builder 阶段）
echo "🔨 步骤 1: 构建 Docker 镜像..."
docker build -f bin/docker/Dockerfile \
    -t ai-interviewer-builder \
    --target builder \
    .

# 创建临时容器并复制产物
echo "📦 步骤 2: 提取编译产物..."
CONTAINER_ID=$(docker create ai-interviewer-builder)
mkdir -p "$PROJECT_ROOT/bin"
docker cp "$CONTAINER_ID:/output/ai-interviewer" "$PROJECT_ROOT/bin/ai-interviewer"
docker rm "$CONTAINER_ID"

# 验证产物
echo "✅ 步骤 3: 验证编译产物..."
if [ -f "$PROJECT_ROOT/bin/ai-interviewer" ]; then
    echo ""
    echo "🎉 编译成功！二进制文件位于：$PROJECT_ROOT/bin/ai-interviewer"
    echo ""
    ls -lh "$PROJECT_ROOT/bin/ai-interviewer"
    echo ""
    echo "📊 文件信息:"
    file "$PROJECT_ROOT/bin/ai-interviewer" || true
    echo ""
    echo "🔗 动态库依赖检查 (应该显示 statically linked):"
    ldd "$PROJECT_ROOT/bin/ai-interviewer" 2>&1 || true
else
    echo "❌ 编译失败：未找到输出文件"
    exit 1
fi
