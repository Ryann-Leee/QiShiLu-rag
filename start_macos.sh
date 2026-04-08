#!/bin/bash
# macOS 一键启动脚本
# 支持 Docker Desktop for Mac (CPU 模式运行 Embedding)

set -e

echo "=========================================="
echo "  Enterprise RAG System - macOS 启动"
echo "=========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    echo "请安装 Docker Desktop for Mac: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# 检查 Docker Desktop 是否运行
if ! docker info &> /dev/null; then
    echo "错误: Docker Desktop 未运行"
    echo "请先启动 Docker Desktop"
    exit 1
fi

# 检查是否是 Apple Silicon (M1/M2/M3)
IS_APPLE_SILICON=$(uname -m)
if [ "$IS_APPLE_SILICON" = "arm64" ]; then
    echo "检测到 Apple Silicon Mac"
    echo "Embedding 服务将使用 CPU 模式运行"
fi

echo ""
echo "启动 Milvus 服务..."
docker-compose up -d milvus etcd minio

echo ""
echo "启动 Embedding 服务 (CPU 模式)..."
docker-compose -f docker-compose.embedding.yml up -d

echo ""
echo "等待服务启动..."
sleep 10

echo ""
echo "=========================================="
echo "  服务启动完成!"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  - Milvus:        localhost:19530"
echo "  - Milvus 监控:   localhost:9091"
echo "  - MinIO:         localhost:9001"
echo "  - Embedding:     localhost:5001"
echo ""
echo "查看状态: docker-compose ps"
echo "查看日志: docker-compose logs -f"
echo "=========================================="
