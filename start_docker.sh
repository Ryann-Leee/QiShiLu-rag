#!/bin/bash
# =============================================================================
# Enterprise RAG System - Docker 一键启动脚本
# =============================================================================
# 功能: 启动所有依赖服务 (Milvus + MySQL + Redis + Embedding)
# 支持: Linux, macOS
# =============================================================================

set -e

echo "=========================================="
echo "  Enterprise RAG System"
echo "  Docker 一键启动脚本"
echo "=========================================="

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    echo "错误: Docker 未运行"
    echo "请先启动 Docker"
    exit 1
fi

# 检查 docker-compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: docker-compose 未安装"
    echo "请先安装 docker-compose: https://docs.docker.com/compose/install/"
    exit 1
fi

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "检测到 .env 文件不存在，复制 .env.docker 作为模板..."
    if [ -f .env.docker ]; then
        cp .env.docker .env
        echo "已创建 .env 文件"
        echo ""
        echo "⚠️  请编辑 .env 文件，配置你的 API Key:"
        echo "   - LLM_API_KEY"
        echo "   - 其他必要的配置"
        echo ""
        read -p "是否现在编辑 .env? (y/n): " edit_now
        if [ "$edit_now" = "y" ] || [ "$edit_now" = "Y" ]; then
            ${EDITOR:-vim} .env
        fi
    else
        echo "错误: .env.docker 也不存在"
        exit 1
    fi
fi

# 创建必要的目录
echo ""
echo "创建数据目录..."
mkdir -p milvus_data/etcd
mkdir -p milvus_data/milvus
mkdir -p milvus_data/minio
mkdir -p milvus_config
mkdir -p models

# 停止旧容器
echo ""
echo "停止旧容器..."
docker-compose down 2>/dev/null || true

# 构建并启动服务
echo ""
echo "构建并启动服务..."
echo "这可能需要几分钟下载镜像..."

docker-compose up -d --build

# 等待服务启动
echo ""
echo "等待服务启动..."
echo "Milvus 首次启动可能需要 1-2 分钟..."

# 等待 Milvus 就绪
MAX_WAIT=180
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if docker-compose ps milvus 2>/dev/null | grep -q "(healthy)"; then
        break
    fi
    echo -n "."
    sleep 5
    WAITED=$((WAITED + 5))
done

# 等待 MySQL 就绪
echo ""
echo "等待 MySQL 就绪..."
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if docker-compose exec -T mysql mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD:-root_password} &>/dev/null; then
        break
    fi
    echo -n "."
    sleep 2
    WAITED=$((WAITED + 2))
done

# 显示服务状态
echo ""
echo ""
echo "=========================================="
echo "  服务启动完成!"
echo "=========================================="
echo ""
docker-compose ps
echo ""
echo "服务地址:"
echo "  - 应用:        http://localhost:5000"
echo "  - Milvus:     localhost:19530"
echo "  - Milvus UI:  http://localhost:9091"
echo "  - MinIO:      http://localhost:9001 (minioadmin/minioadmin)"
echo "  - MySQL:      localhost:3306"
echo "  - Redis:      localhost:6379"
echo "  - Embedding:  http://localhost:5001"
echo ""
echo "=========================================="
echo "管理命令:"
echo "  查看状态:   docker-compose ps"
echo "  查看日志:   docker-compose logs -f [service]"
echo "  停止服务:   docker-compose down"
echo "  清理数据:   docker-compose down -v"
echo "=========================================="
