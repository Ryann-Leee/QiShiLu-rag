# QiShiLu-RAG - AGENTS.md

## 1. 项目概览

### 项目名称
Enterprise RAG System

### 项目描述
企业级 RAG（检索增强生成）系统，支持多租户隔离、长期记忆、语义分片文档上传及统一环境变量管理。

### 技术栈
- **后端**: Java 21, Spring Boot 3.2.5, Milvus SDK 2.5.3, MySQL 8.0, dotenv-java 3.0.2
- **前端**: Next.js 16, React 19, TypeScript, shadcn/ui, Tailwind CSS 4
- **向量数据库**: Milvus 2.5.x (支持 Docker / Lite / 云版本)
- **缓存**: Redis
- **编码规范**: Airbnb (Java), Standard (TypeScript)

### 核心依赖
- Spring Boot 3.2.5
- Spring Data JPA
- Spring Data Redis
- Milvus SDK 2.5.3 (支持 BM25 Hybrid Search)
- dotenv-java 3.0.2 (环境变量管理)
- jtokkit 0.6.0 (Token 计数)
- Gson 2.10.1 (JSON 处理)
- SpringDoc OpenAPI (API 文档)

---

## 2. 构建和测试命令

### 构建命令
```bash
cd /workspace/projects/rag-system
mvn clean package -DskipTests
```

### 运行命令
```bash
# 方式一：使用 Docker 启动 Milvus (推荐)
cp .env.docker .env
./start_docker.sh

# 或直接
docker-compose up -d

# 方式二：本地启动 Milvus (需要安装 milvus-lite)
pip install milvus-lite==2.5.1
python start_milvus.py

# 启动 MySQL 和 Redis
service mysql start
service redis-server start

# 启动应用
java -jar target/rag-system-1.0.0.jar
```

### Docker 部署 (推荐)

```bash
# 启动 Milvus + etcd + MinIO
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f milvus

# 停止
docker-compose down
```

### 测试命令
```bash
# 单元测试
mvn test

# API 测试
curl -X POST http://localhost:5000/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default-tenant" \
  -d '{"message": "Hello", "sessionId": "test-session"}'
```

### 端口
- **应用端口**: 5000
- **MySQL 端口**: 3306
- **Redis 端口**: 6379
- **Milvus 端口**: 19530
- **Milvus 监控**: 9091
- **MinIO 控制台**: 9001

---

## 3. 项目结构

```
rag-system/
├── .env                          # 环境变量配置文件
├── .env.example                  # 环境变量模板
├── .env.docker                   # Docker 环境变量模板
├── .gitignore                    # Git 忽略规则
├── pom.xml                       # Maven 依赖配置
├── docker-compose.yml            # Docker 部署配置 (Milvus + etcd + MinIO)
├── start_docker.sh               # Docker 一键启动脚本
├── start_milvus.py               # Milvus Lite 启动脚本
├── milvus_config/                # Milvus 配置文件目录
│   └── milvus.yaml
├── ENVIRONMENT_CONFIG.md         # 配置指南文档
├── README.md                     # 项目文档
└── src/
    └── main/
        ├── java/com/enterprise/rag/
        │   ├── RagSystemApplication.java
        │   ├── config/
        │   │   ├── ApplicationStartupConfig.java  # 启动配置，加载 .env
        │   │   ├── EnvConfig.java                  # 环境变量加载器
        │   │   ├── MilvusConfig.java               # Milvus V2 客户端配置
        │   │   ├── SecurityConfig.java
        │   │   └── WebConfig.java
        │   ├── controller/
        │   │   ├── ChatController.java             # 聊天接口
        │   │   ├── ChunkMetadataController.java
        │   │   ├── DocumentUploadController.java   # 文档上传
        │   │   ├── KnowledgeController.java
        │   │   └── TenantController.java
        │   ├── dto/
        │   │   ├── ChatRequest.java
        │   │   ├── ChatResponse.java
        │   │   ├── DocumentRequest.java
        │   │   ├── FileUploadRequest.java
        │   │   ├── TenantRequest.java
        │   │   └── TenantResponse.java
        │   ├── entity/
        │   │   ├── ChunkMetadata.java
        │   │   ├── Conversation.java
        │   │   ├── EpisodicMemory.java
        │   │   ├── KnowledgeDocument.java
        │   │   ├── Message.java
        │   │   ├── ProcessStatus.java
        │   │   ├── Tenant.java
        │   │   ├── TenantIsolationLevel.java
        │   │   └── UserProfile.java
        │   ├── memory/
        │   │   ├── MemoryContext.java
        │   │   └── MemoryService.java
        │   ├── milvus/
        │   │   ├── MilvusService.java               # 向量数据库服务 (旧版/内存模拟)
        │   │   └── MilvusDualEngineService.java     # 双路混合检索服务 (V2.5+)
        │   ├── processor/
        │   │   └── SemanticOverlapTextSplitter.java # 语义分片处理器
        │   ├── repository/
        │   │   ├── ChunkMetadataRepository.java
        │   │   ├── ConversationRepository.java
        │   │   ├── EpisodicMemoryRepository.java
        │   │   ├── KnowledgeDocumentRepository.java
        │   │   ├── MessageRepository.java
        │   │   ├── TenantRepository.java
        │   │   └── UserProfileRepository.java
        │   ├── service/
        │   │   ├── DocumentIngestionService.java    # 文档处理服务
        │   │   ├── LLMService.java                  # LLM 服务
        │   │   └── RagService.java
        │   ├── tenant/
        │   │   ├── TenantContext.java
        │   │   ├── TenantFilter.java
        │   │   └── TenantService.java
        │   └── util/
        │       └── ChunkMetadataDemoData.java
        └── resources/
            ├── application.yml                       # Spring 配置
            └── schema.sql                            # 数据库 schema
```

---

## 4. 环境变量配置

### 配置文件位置
- **主配置文件**: `.env`
- **模板文件**: `.env.example`
- **配置文档**: `ENVIRONMENT_CONFIG.md`

### 配置分类

#### LLM 配置
| 变量名 | 说明 | 示例 |
|--------|------|------|
| `LLM_PROVIDER` | LLM 提供商 | openai |
| `LLM_MODEL` | LLM 模型 | gpt-4o-mini |
| `LLM_API_KEY` | LLM API Key | sk-xxx |
| `LLM_BASE_URL` | LLM API 地址 | https://api.openai.com/v1 |
| `LLM_MAX_TOKENS` | 最大 Token 数 | 4096 |
| `LLM_TEMPERATURE` | 温度参数 | 0.7 |

#### Embedding 配置
| 变量名 | 说明 | 示例 |
|--------|------|------|
| `EMBEDDING_PROVIDER` | Embedding 提供商 | local / openai |
| `EMBEDDING_MODEL` | Embedding 模型 | Qwen3-Embedding-0.6B |
| `EMBEDDING_DIMENSION` | 向量维度 | 1024 |
| `EMBEDDING_LOCAL_URL` | 本地服务地址 | http://localhost:5001 |
| `EMBEDDING_API_KEY` | API Key (OpenAI) | ${LLM_API_KEY} |
| `EMBEDDING_BASE_URL` | API 地址 (OpenAI) | https://api.openai.com/v1 |

#### Milvus 配置
| 变量名 | 说明 | 示例 |
|--------|------|------|
| `MILVUS_HOST` | Milvus 主机 | localhost |
| `MILVUS_PORT` | Milvus 端口 | 19530 |
| `MILVUS_DATABASE` | 数据库名 | default |
| `MILVUS_USERNAME` | 用户名 | (可选) |
| `MILVUS_PASSWORD` | 密码 | (可选) |
| `MILVUS_TIMEOUT` | 超时时间(ms) | 5000 |

#### 混合检索配置 (Milvus 2.5+)
| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `HYBRID_SEARCH_ENABLED` | 启用混合检索 | true |
| `HYBRID_SEARCH_DENSE_TOP_K` | 稠密向量召回数 | 100 |
| `HYBRID_SEARCH_SPARSE_TOP_K` | 稀疏向量召回数 | 100 |
| `HYBRID_SEARCH_RRF_K` | RRF 融合参数 | 60 |
| `HYBRID_SEARCH_FINAL_TOP_K` | 最终返回数 | 10 |

#### MySQL 配置
| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_HOST` | MySQL 主机 | localhost |
| `DB_PORT` | MySQL 端口 | 3306 |
| `DB_NAME` | 数据库名 | rag_db |
| `DB_USERNAME` | 用户名 | rag_user |
| `DB_PASSWORD` | 密码 | rag_password |

#### Redis 配置
| 变量名 | 说明 | 示例 |
|--------|------|------|
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | 密码 | (可选) |
| `REDIS_DATABASE` | 数据库编号 | 0 |
| `REDIS_TIMEOUT` | 超时时间(ms) | 3000 |

#### 分片配置
| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPLITTER_CHUNK_SIZE` | 分片大小(Token) | 500 |
| `SPLITTER_OVERLAP_TOKENS` | 重叠Token数 | 100 |
| `SPLITTER_MIN_CHUNK_SIZE` | 最小分片大小 | 50 |

---

## 5. API 端点

### 聊天接口
- `POST /api/chat` - 发送聊天消息

### 文档接口
- `POST /api/documents/upload` - 上传文档
- `GET /api/documents/{id}` - 获取文档信息
- `GET /api/documents/{id}/chunks` - 获取文档分块

### 知识库接口
- `POST /api/knowledge/query` - 知识库查询
- `GET /api/knowledge/stats` - 知识库统计

### 租户接口
- `POST /api/tenants` - 创建租户
- `GET /api/tenants/{id}` - 获取租户信息
- `PUT /api/tenants/{id}` - 更新租户
- `DELETE /api/tenants/{id}` - 删除租户

---

## 6. 核心功能实现

### 6.1 环境变量加载
- 使用 `dotenv-java` 库加载 `.env` 文件
- `EnvConfig.java` 在应用启动时加载环境变量
- `ApplicationStartupConfig.java` 打印配置摘要（敏感信息已掩码）

### 6.2 多租户隔离
- 三级隔离策略：Collection > Partition > Metadata
- 默认使用 Metadata 级别隔离
- 通过 `TenantContext` 和 `TenantFilter` 实现

### 6.3 多租户隔离
- 三级隔离策略：Collection > Partition > Metadata
- 默认使用 Metadata 级别隔离
- 通过 `TenantContext` 和 `TenantFilter` 实现
- 检索前预过滤：使用 `tenant_id` 字段过滤

### 6.4 长期记忆
- **Session Memory**: 会话级记忆，存储在 MySQL messages 表
- **Episodic Memory**: 情景记忆，存储在 MySQL + Milvus (当前使用内存模拟)
- **User Profile**: 用户画像，定时更新

### 6.5 语义分片
- 基于余弦相似度的智能分片
- 支持 100 Token 重叠
- 使用 `jtokkit` 进行 Token 计数

### 6.6 双路混合检索 (Milvus 2.5+)
- **Dense Vector**: Embedding 语义向量 (COSINE)
- **Sparse Vector**: BM25 关键词向量 (auto-analyzer)
- **RRF 融合**: Reciprocal Rank Fusion 无参数融合
- 由 `MilvusDualEngineService` 实现

---

## 7. 安全注意事项

1. **敏感信息保护**: `.env` 文件已添加到 `.gitignore`，不会提交到代码库
2. **API Key 掩码**: 日志中敏感信息自动掩码显示
3. **CORS 配置**: 支持配置允许的跨域来源
4. **租户隔离**: 确保不同租户数据严格隔离

---

## 8. 常见问题

### Q: 如何修改配置？
A: 编辑 `.env` 文件，修改对应变量值后重启应用。

### Q: 如何查看加载的配置？
A: 应用启动时会打印配置摘要到日志。

### Q: 如何使用不同的 LLM 提供商？
A: 修改 `LLM_PROVIDER`、`LLM_BASE_URL` 和 `LLM_API_KEY`。

### Q: 如何调整分片参数？
A: 修改 `SPLITTER_CHUNK_SIZE` 和 `SPLITTER_OVERLAP_TOKENS`。

### Q: Docker 部署 Milvus 失败？
A: 检查 Docker 是否运行，确保端口 19530、9091、9001 未被占用。

### Q: 如何使用 Docker 快速启动 Milvus？
```bash
# 方式一：使用脚本
./start_docker.sh

# 方式二：直接 docker-compose
docker-compose up -d
```

### Q: Milvus Lite 在沙箱环境无法启动？
A: Milvus Lite 需要系统底层资源，沙箱环境有限制。建议使用 Docker 部署或 Zilliz Cloud。

---

## 9. 维护说明

### 日志位置
- `/app/work/logs/bypass/app.log` - 应用日志

### 数据库维护
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS rag_db;
CREATE USER IF NOT EXISTS 'rag_user'@'localhost' IDENTIFIED BY 'rag_password';
GRANT ALL PRIVILEGES ON rag_db.* TO 'rag_user'@'localhost';
```

### 服务重启
```bash
# 停止 Java 应用
pkill -f rag-system-1.0.0.jar

# 重启 Docker Milvus
docker-compose restart milvus

# 或完全重建
docker-compose down && docker-compose up -d

# 启动数据库和 Redis
service mysql start
service redis-server start

# 启动应用
java -jar target/rag-system-1.0.0.jar > /app/work/logs/bypass/app.log 2>&1 &
```
