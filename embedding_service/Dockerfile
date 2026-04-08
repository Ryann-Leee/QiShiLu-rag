# Qwen3-Embedding-0.6B Docker 镜像
# 需要 NVIDIA GPU 支持

FROM nvidia/cuda:12.1.0-runtime-ubuntu22.04

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    python3.10 \
    python3-pip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置 Python 路径
RUN ln -sf /usr/bin/python3.10 /usr/bin/python

# 设置 HF Mirror
ENV HF_ENDPOINT=https://hf-mirror.com
ENV PYTHONUNBUFFERED=1

# 复制依赖文件
COPY requirements-embedding.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements-embedding.txt

# 复制服务代码
COPY embedding_service.py .
COPY download_model.py .

# 创建模型目录
RUN mkdir -p /app/models

# 暴露端口
EXPOSE 5001

# 启动命令
# 默认从 HuggingFace 下载模型
# 如需使用本地模型，挂载 models 目录
CMD ["python", "embedding_service.py", "--port", "5001", "--host", "0.0.0.0"]
