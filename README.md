# Enterprise RAG System

<div align="center">

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Milvus](https://img.shields.io/badge/Milvus-2.5.3-orange.svg)](https://milvus.io/)

企业级 RAG（检索增强生成）系统，支持多租户隔离、长期记忆、语义分片文档上传

[English](./README_EN.md) | 简体中文

</div>

---

## 目录

- [特性](#特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [项目结构](#项目结构)
- [API 文档](#api-文档)
- [核心功能](#核心功能)
- [安全说明](#安全说明)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 特性

### 多租户隔离
- 三级隔离策略：Collection / Partition / Metadata
- 默认采用 Metadata 级别隔离，确保数据安全
- 支持租户级别的独立配置

### 长期记忆
- **会话记忆 (Session Memory)**: 基于 Redis 的实时会话存储
- **情景记忆 (Episodic Memory)**: 重要对话片段的长期存储
- **用户画像 (User Profile)**: 用户偏好自动学习与更新

### 语义分片
- 基于余弦相似度的智能文档分片
- 可配置的分片大小和重叠 Token 数
- 支持 100 Token 重叠，确保上下文连贯性

### 统一配置管理
- 使用 `.env` 文件集中管理所有配置
- 区分不同服务的配置（LLM、Embedding、Milvus、MySQL、Redis）
- 敏感信息自动掩码保护

### 企业级功能
- [x] 多租户数据隔离
- [x] 对话历史管理
- [x] 文档上传与处理
- [x] 向量检索
- [x] RAG 问答
- [x] 长期记忆
- [x] 语义分片
- [x] 统一环境变量配置
- [x] RESTful API
- [x] 双路混合检索 (Dense + BM25 + RRF)
- [x] 本地 Embedding 服务 (Qwen3-Embedding-0.6B)
- [ ] WebSocket 实时对话
- [ ] 前端界面
- [ ] 鉴权与用户管理

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Data JPA | 3.x | ORM 框架 |
| Spring Data Redis | 3.x | 缓存访问 |
| Milvus SDK | 2.5.3 | 向量数据库 (支持 BM25 Hybrid Search) |
| MySQL | 8.0 | 关系数据库 |
| Redis | - | 缓存服务 |
| dotenv-java | 3.0.2 | 环境变量管理 |
| jtokkit | 0.6.0 | Token 计数 |

### 前端 (规划中)

| 技术 | 版本 | 说明 |
|------|------|------|
| Next.js | 16 | React 框架 |
| React | 19 | UI 库 |
| TypeScript | 5 | 类型系统 |
| shadcn/ui | - | UI 组件库 |
| Tailwind CSS | 4 | 样式框架 |

---

## 快速开始

### 前置要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Milvus 2.5+ (支持 BM25 Function)

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/your-org/enterprise-rag-system.git
cd enterprise-rag-system
```

#### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，配置你的服务
vim .env
```

#### 3. 启动依赖服务

```bash
# 启动 MySQL
service mysql start

# 初始化数据库
mysql -u root -e "
CREATE DATABASE IF NOT EXISTS rag_db;
CREATE USER IF NOT EXISTS 'rag_user'@'localhost' IDENTIFIED BY 'rag_password';
GRANT ALL PRIVILEGES ON rag_db.* TO 'rag_user'@'localhost';
FLUSH PRIVILEGES;
"

# 启动 Redis
service redis-server start
```

#### 4. 构建项目

```bash
mvn clean package -DskipTests
```

#### 5. 启动应用

```bash
java -jar target/rag-system-1.0.0.jar
```

应用将在 `http://localhost:5000` 启动。

### Docker 一键部署（推荐）

使用 Docker 一键启动所有依赖服务：

```bash
# 1. 复制环境变量模板
cp .env.docker .env

# 2. 编辑 .env 配置（必须设置 LLM_API_KEY）
vim .env

# 3. 一键启动所有服务
./start_docker.sh

# 或直接使用 docker-compose
docker-compose up -d --build
```

**启动后服务地址**：

| 服务 | 地址 | 说明 |
|------|------|------|
| 应用 | http://localhost:5000 | RAG 系统 API |
| Milvus | localhost:19530 | 向量数据库 |
| Milvus UI | http://localhost:9091 | Milvus 监控面板 |
| MinIO | http://localhost:9001 | 对象存储控制台 |
| MySQL | localhost:3306 | 关系数据库 |
| Redis | localhost:6379 | 缓存服务 |
| Embedding | http://localhost:5001 | 本地向量服务 |

**管理命令**：
```bash
# 查看所有服务状态
docker-compose ps

# 查看某个服务日志
docker-compose logs -f milvus
docker-compose logs -f mysql
docker-compose logs -f redis

# 停止所有服务（保留数据）
docker-compose down

# 完全清理（删除所有数据）
docker-compose down -v

# 重启某个服务
docker-compose restart milvus
```

**首次启动注意事项**：
- Milvus 首次启动需要 1-2 分钟初始化
- MySQL 会自动执行 `init.sql` 初始化数据库
- Embedding 服务首次会下载模型（约 1.2GB）

---

### 本地 Embedding 服务 (Qwen3-Embedding-0.6B)

使用本地模型替代 OpenAI API，支持中英文 embedding：

#### 方式一：本地 Python 服务（推荐）

```bash
# 1. 安装依赖
pip install -r requirements-embedding.txt

# 2. 下载模型 (约 1.2GB)
python download_model.py

# 或直接从 HuggingFace 下载
export HF_ENDPOINT=https://hf-mirror.com
huggingface-cli download Qwen/Qwen3-Embedding-0.6B --local-dir ./models/Qwen3-Embedding-0.6B

# 3. 启动服务
python embedding_service.py --port 5001

# 4. 测试
curl -X POST http://localhost:5001/embed \
  -H "Content-Type: application/json" \
  -d '{"texts": ["你好，世界"]}'
```

#### 方式二：Docker 部署

**GPU 版本（推荐）**：
```bash
# 启动所有服务（包括 Milvus + Embedding）
docker-compose up -d

# 或只启动 embedding 服务
docker-compose up -d embedding
```

**CPU 版本（没有 GPU）**：
```bash
# 使用 CPU 版本的 compose 文件
docker-compose -f docker-compose.embedding.yml up -d
```

#### API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/embed` | POST | 获取文本 embedding |
| `/embed_batch` | POST | 批量获取 embedding |
| `/similarity` | POST | 计算文本相似度 |
| `/health` | GET | 健康检查 |

### macOS 部署

Docker Desktop for Mac 原生支持，一键启动：

```bash
# 方法一：使用启动脚本（推荐）
./start_macos.sh

# 方法二：手动启动
# 1. 启动 Milvus
docker-compose up -d milvus etcd minio

# 2. 启动 Embedding (CPU 模式)
docker-compose -f docker-compose.embedding.yml up -d
```

**注意**：macOS 不支持 NVIDIA GPU，Embedding 服务将使用 CPU 模式运行。

---

### 本地开发 (不使用 Docker)

#### 1. 克隆项目

```bash
git clone https://github.com/your-org/enterprise-rag-system.git
cd enterprise-rag-system
```

#### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，配置你的服务
vim .env
```

#### 3. 启动依赖服务

```bash
# 启动 MySQL
service mysql start

# 初始化数据库
mysql -u root -e "
CREATE DATABASE IF NOT EXISTS rag_db;
CREATE USER IF NOT EXISTS 'rag_user'@'localhost' IDENTIFIED BY 'rag_password';
GRANT ALL PRIVILEGES ON rag_db.* TO 'rag_user'@'localhost';
FLUSH PRIVILEGES;
"

# 启动 Redis
service redis-server start
```

#### 4. 启动 Milvus

```bash
# 使用 Python 脚本（需要 milvus-lite）
pip install milvus-lite
python start_milvus.py
```

#### 5. 构建项目

```bash
mvn clean package -DskipTests
```

#### 6. 启动应用

```bash
java -jar target/rag-system-1.0.0.jar
```

应用将在 `http://localhost:5000` 启动。

---

## 配置说明

### 环境变量配置

所有配置通过 `.env` 文件管理，详见 [ENVIRONMENT_CONFIG.md](./ENVIRONMENT_CONFIG.md)。

#### LLM 配置

```env
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o-mini
LLM_API_KEY=your_api_key
LLM_BASE_URL=https://api.openai.com/v1
LLM_MAX_TOKENS=4096
LLM_TEMPERATURE=0.7
```

#### Embedding 配置

```env
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_API_KEY=${LLM_API_KEY}
EMBEDDING_BASE_URL=https://api.openai.com/v1
EMBEDDING_DIMENSION=1536
```

#### 向量数据库配置

```env
# Milvus (本地)
MILVUS_HOST=localhost
MILVUS_PORT=19530
MILVUS_DATABASE=default

# 或 Milvus Cloud
# MILVUS_CLOUD_ENDPOINT=your-endpoint.zillizcloud.com
# MILVUS_CLOUD_TOKEN=your_token
```

#### 混合检索配置 (Milvus 2.5+)

```env
# 混合检索配置
HYBRID_SEARCH_ENABLED=true
HYBRID_SEARCH_DENSE_TOP_K=100        # 稠密向量召回数量
HYBRID_SEARCH_SPARSE_TOP_K=100       # 稀疏向量(BM25)召回数量
HYBRID_SEARCH_RRF_K=60               # RRF 融合后最终返回数量
```

#### 关系数据库配置

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=rag_db
DB_USERNAME=rag_user
DB_PASSWORD=rag_password
```

#### Redis 配置

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
```

#### 分片配置

```env
SPLITTER_CHUNK_SIZE=500
SPLITTER_OVERLAP_TOKENS=100
SPLITTER_MIN_CHUNK_SIZE=50
```

---

## 项目结构

```
enterprise-rag-system/
├── .env                          # 环境变量配置文件
├── .env.example                  # 环境变量模板
├── .gitignore                    # Git 忽略规则
├── pom.xml                       # Maven 依赖配置
├── README.md                     # 项目说明文档
├── README_EN.md                  # English README
├── LICENSE                       # 许可证
├── ENVIRONMENT_CONFIG.md         # 配置指南
├── AGENTS.md                     # Agent 开发规范
└── src/
    └── main/
        ├── java/com/enterprise/rag/
        │   ├── RagSystemApplication.java
        │   ├── config/                       # 配置类
        │   │   ├── ApplicationStartupConfig.java
        │   │   ├── EnvConfig.java
        │   │   ├── MilvusConfig.java         # Milvus V2 客户端配置
        │   │   ├── SecurityConfig.java
        │   │   └── WebConfig.java
        │   ├── controller/                  # REST 控制器
        │   │   ├── ChatController.java
        │   │   ├── ChunkMetadataController.java
        │   │   ├── DocumentUploadController.java
        │   │   ├── KnowledgeController.java
        │   │   └── TenantController.java
        │   ├── dto/                         # 数据传输对象
        │   │   ├── ChatRequest.java
        │   │   ├── ChatResponse.java
        │   │   ├── DocumentRequest.java
        │   │   ├── FileUploadRequest.java
        │   │   ├── TenantRequest.java
        │   │   └── TenantResponse.java
        │   ├── entity/                      # 数据实体
        │   │   ├── ChunkMetadata.java
        │   │   ├── Conversation.java
        │   │   ├── EpisodicMemory.java
        │   │   ├── KnowledgeDocument.java
        │   │   ├── Message.java
        │   │   ├── Tenant.java
        │   │   └── UserProfile.java
        │   ├── memory/                      # 记忆服务
        │   │   ├── MemoryContext.java
        │   │   └── MemoryService.java
        │   ├── milvus/                      # 向量数据库
        │   │   ├── MilvusService.java        # 向量检索服务
        │   │   └── MilvusDualEngineService.java  # 双路混合检索服务
        │   ├── processor/                   # 文档处理器
        │   │   └── SemanticOverlapTextSplitter.java
        │   ├── repository/                  # 数据仓库
        │   ├── service/                     # 业务服务
        │   │   ├── DocumentIngestionService.java
        │   │   ├── LLMService.java
        │   │   └── RagService.java
        │   └── tenant/                      # 多租户支持
        │       ├── TenantContext.java
        │       ├── TenantFilter.java
        │       └── TenantService.java
        └── resources/
            └── application.yml              # Spring 配置
```

---

## API 文档

### 聊天接口

#### 发送消息

```http
POST /api/chat
Content-Type: application/json
X-Tenant-ID: your-tenant-id

{
  "message": "你好，请介绍一下你们的产品",
  "sessionId": "session-123"
}
```

**响应**

```json
{
  "sessionId": "session-123",
  "message": "您好！我们的产品是一款...",
  "timestamp": "2024-01-15T10:30:00Z",
  "contextChunks": 3
}
```

### 文档接口

#### 上传文档

```http
POST /api/documents/upload
Content-Type: multipart/form-data
X-Tenant-ID: your-tenant-id

file: (binary)
title: "产品手册"
description: "2024年产品介绍"
```

#### 获取文档列表

```http
GET /api/documents
X-Tenant-ID: your-tenant-id
```

### 租户接口

#### 创建租户

```http
POST /api/tenants
Content-Type: application/json

{
  "name": "Acme Corporation",
  "isolationLevel": "METADATA",
  "description": "Acme 公司专用实例"
}
```

---

## 核心功能

### 多租户隔离

系统支持三种租户隔离级别：

| 隔离级别 | 说明 | 适用场景 |
|---------|------|---------|
| `COLLECTION` | 每个租户独立 Collection | 极高安全要求 |
| `PARTITION` | 每个租户独立 Partition | 高并发场景 |
| `METADATA` | 通过 Metadata 字段隔离 (默认) | 一般企业使用 |

### 长期记忆架构

```
┌─────────────────────────────────────────────────────┐
│                   用户对话输入                        │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│              Session Memory (Redis)                  │
│         最近 20 条对话，TTL 30 分钟                  │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│            Episodic Memory (MySQL)                  │
│         Top-5 重要片段，阈值 > 0.3                   │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│              User Profile (MySQL)                   │
│           每 5 次对话更新一次偏好                    │
└─────────────────────────────────────────────────────┘
```

### 语义分片算法

```
1. 文本预处理 → 句子分割
2. 计算相邻句子的语义相似度 (余弦相似度)
3. 如果相似度 < 阈值，开始新分片
4. 否则，将句子加入当前分片
5. 添加重叠 Token (默认 100)
6. 重复直到处理完所有文本
```

### 双路混合检索 (Milvus 2.5+)

系统采用**稠密向量 + 稀疏向量 (BM25)** 双路召回策略，通过 **RRF (Reciprocal Rank Fusion)** 算法融合结果：

```
┌─────────────────────────────────────────────────────┐
│                    用户查询输入                       │
└─────────────────────────────────────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          ▼                              ▼
┌─────────────────────┐    ┌─────────────────────┐
│   Dense Vector      │    │   Sparse Vector     │
│   (语义相似度)       │    │   (BM25 关键词匹配) │
│   COSINE 检索        │    │   自动生成稀疏向量   │
└─────────────────────┘    └─────────────────────┘
          │                              │
          └──────────────┬──────────────┘
                         ▼
┌─────────────────────────────────────────────────────┐
│              RRF (Reciprocal Rank Fusion)            │
│   score = Σ 1/(k + rank_i)  [k=60]                │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
              最终融合结果 (Top-K)
```

**技术特点**:
- **稠密向量**: 使用 Embedding 模型生成语义向量，捕捉深层语义关系
- **稀疏向量 (BM25)**: 基于词频统计的经典检索算法，精确匹配关键词
- **RRF 融合**: 无需训练参数，平衡语义理解和关键词匹配

---

## 安全说明

### 敏感信息保护

- `.env` 文件已添加到 `.gitignore`，不会提交到代码库
- 日志中 API Key 等敏感信息自动掩码显示
- 建议生产环境使用环境变量而非 `.env` 文件

### 租户数据隔离

- 所有数据库操作自动携带租户 ID
- 向量数据库检索限制在当前租户范围内
- API 请求需要携带 `X-Tenant-ID` 头

---

## 贡献指南

欢迎提交 Pull Request 或 Issue！

### 开发环境设置

```bash
# 克隆仓库
git clone https://github.com/your-org/enterprise-rag-system.git

# 安装 JDK 21
# 请参考: https://adoptium.net/

# 安装 Maven
# 请参考: https://maven.apache.org/install.html

# 运行测试
mvn test

# 代码格式化 (Airbnb Java Style)
mvn spotless:apply
```

### 提交规范

请使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat: 添加新功能
fix: 修复问题
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具相关
```

---

## 许可证

本项目基于 MIT 许可证开源，详见 [LICENSE](./LICENSE) 文件。

---

## 联系方式

- GitHub Issues: [https://github.com/your-org/enterprise-rag-system/issues](https://github.com/your-org/enterprise-rag-system/issues)
- Email: your.email@example.com

---

<div align="center">

**如果这个项目对你有帮助，请给一个 Star ⭐**

</div>
