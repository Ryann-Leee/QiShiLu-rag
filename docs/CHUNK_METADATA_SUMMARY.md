# 分片元数据 MySQL 存储实现总结

## 概述

已实现将文档分片元数据保存到 MySQL 数据库的功能，包括分片之间的关联关系。

## 实现的功能

### 1. 数据库实体

创建了 `ChunkMetadata` 实体类，包含以下字段：

```java
public class ChunkMetadata {
    private String id;              // 主键
    private String docId;           // 文档ID
    private String chunkId;         // chunk唯一ID
    private String prevChunkId;     // 前一个chunk的ID
    private String nextChunkId;     // 后一个chunk的ID
    private Integer tokenCount;     // token数量
    private String pageRange;       // 来源页码范围
    private String content;         // chunk内容
    private Integer chunkIndex;     // chunk在文档中的索引
    private String tenantId;        // 租户ID
    private Instant createdAt;      // 创建时间
    private Instant updatedAt;      // 更新时间
}
```

### 2. Repository 接口

`ChunkMetadataRepository` 提供以下查询方法：

- `findByDocIdOrderByChunkIndex()` - 按文档ID查询所有分片
- `findByChunkId()` - 按chunk ID查询
- `findByDocIdAndTenantIdOrderByChunkIndex()` - 按文档ID和租户ID查询
- `countByDocId()` - 统计文档的分片数量
- `deleteByDocId()` - 删除文档的所有分片
- `findFirstByDocIdOrderByChunkIndexAsc()` - 获取第一个分片
- `findFirstByDocIdOrderByChunkIndexDesc()` - 获取最后一个分片

### 3. 自动保存流程

在 `DocumentIngestionService.processTextDocument()` 中自动保存元数据：

```java
// 1. 语义分片
List<SemanticOverlapTextSplitter.DocumentChunk> chunks = 
    textSplitter.splitDocument(document);

// 2. 保存到 Milvus + MySQL
for (int i = 0; i < chunks.size(); i++) {
    // 保存向量到 Milvus
    String vectorId = milvusService.insertDocument(...);
    
    // 创建元数据
    String prevChunkId = (i > 0) ? chunks.get(i - 1).getId() : null;
    String nextChunkId = (i < chunks.size() - 1) ? chunks.get(i + 1).getId() : null;
    
    ChunkMetadata metadata = ChunkMetadata.builder()
            .docId(docId)
            .chunkId(chunk.getId())
            .prevChunkId(prevChunkId)
            .nextChunkId(nextChunkId)
            .tokenCount(chunk.getTokenCount())
            .pageRange("1")
            .content(chunk.getContent())
            .chunkIndex(chunk.getChunkIndex())
            .tenantId(tenantId)
            .build();
    
    chunkMetadataList.add(metadata);
}

// 批量保存
chunkMetadataRepository.saveAll(chunkMetadataList);
```

### 4. API 端点

创建了 `ChunkMetadataController` 提供查询接口：

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/chunks/document/{docId}` | GET | 获取文档的所有分片 |
| `/api/chunks/{chunkId}` | GET | 获取指定分片 |
| `/api/chunks/document/{docId}/stats` | GET | 获取分片统计信息 |
| `/api/chunks/tenant` | GET | 获取租户的所有分片 |

### 5. MySQL 配置

#### 方式 1: 使用 profile

```yaml
# application-mysql.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_db?useSSL=false&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: rag_user
    password: your_password

  jpa:
    hibernate:
      ddl-auto: update  # 自动创建表
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

启动时使用：
```bash
java -jar app.jar --spring.profiles.active=mysql
```

#### 方式 2: 直接修改 application.yml

注释掉 H2 配置，启用 MySQL 配置。

## 数据库表结构

```sql
CREATE TABLE chunk_metadata (
    id VARCHAR(255) PRIMARY KEY,
    doc_id VARCHAR(255) NOT NULL,
    chunk_id VARCHAR(255) NOT NULL UNIQUE,
    prev_chunk_id VARCHAR(255),
    next_chunk_id VARCHAR(255),
    token_count INT NOT NULL,
    page_range VARCHAR(50),
    content TEXT,
    chunk_index INT NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_doc_id (doc_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_chunk_index (chunk_index),
    
    FOREIGN KEY (doc_id) REFERENCES knowledge_document(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
```

## 使用示例

### 1. 查询文档的所有分片

```bash
curl -X GET http://localhost:5001/api/chunks/document/demo-doc-001 \
  -H "X-Tenant-ID: demo-tenant" \
  -H "X-User-ID: user-001"
```

响应：
```json
{
  "docId": "demo-doc-001",
  "count": 5,
  "chunks": [
    {
      "id": "...",
      "docId": "demo-doc-001",
      "chunkId": "xxx",
      "prevChunkId": null,
      "nextChunkId": "yyy",
      "tokenCount": 100,
      "pageRange": "1",
      "content": "...",
      "chunkIndex": 0,
      "tenantId": "demo-tenant"
    },
    ...
  ]
}
```

### 2. 查询分片统计信息

```bash
curl -X GET http://localhost:5001/api/chunks/document/demo-doc-001/stats \
  -H "X-Tenant-ID: demo-tenant"
```

响应：
```json
{
  "docId": "demo-doc-001",
  "totalChunks": 5,
  "totalTokens": 750,
  "avgTokens": "150.00",
  "maxTokens": 300,
  "minTokens": 100
}
```

## 特性

### ✅ 已实现

- ✅ 自动保存分片元数据
- ✅ 分片关联关系 (prev/next)
- ✅ 多租户隔离
- ✅ Token 统计
- ✅ 页码范围记录
- ✅ RESTful API 查询接口
- ✅ 批量保存优化
- ✅ 外键约束和级联删除

### 📝 注意事项

1. **数据库选择**：
   - 开发环境：使用 H2 (无需配置)
   - 生产环境：使用 MySQL (更稳定、性能更好)

2. **page_range 字段**：
   - 当前默认为 "1"
   - 生产环境需要从文档解析实际页码

3. **级联删除**：
   - 删除文档时，相关分片会自动删除

4. **索引优化**：
   - 已为 `doc_id`、`tenant_id`、`chunk_index` 创建索引
   - 支持高效查询

## 下一步优化建议

1. **页码解析**：实现从 PDF 文档提取实际页码范围
2. **分片内容摘要**：为每个分片生成简短摘要
3. **分片质量评分**：评估分片质量（完整性、连贯性）
4. **分片版本控制**：支持分片历史版本
5. **批量查询优化**：使用游标分页处理大量数据

## 相关文件

- `ChunkMetadata.java` - 实体类
- `ChunkMetadataRepository.java` - 数据访问层
- `DocumentIngestionService.java` - 业务逻辑（已更新）
- `ChunkMetadataController.java` - REST API
- `MYSQL_SETUP.md` - MySQL 配置指南
- `schema.sql` - 数据库表结构
