# Environment Configuration Guide

## Overview

All configuration is managed through a single `.env` file. Different types of models and services are clearly separated.

## File Structure

```
rag-system/
├── .env              # Actual configuration (contains secrets, not committed)
├── .env.example      # Template with all configuration options
└── .gitignore        # .env is ignored by git
```

## Quick Start

1. Copy the template:
   ```bash
   cp .env.example .env
   ```

2. Fill in your actual values:
   ```bash
   nano .env
   ```

3. The application will automatically load configuration from `.env`

## Configuration Categories

### 1. LLM (Large Language Model)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `LLM_PROVIDER` | LLM provider name | `openai` | Yes |
| `LLM_MODEL` | Chat completion model | `gpt-4o-mini` | Yes |
| `LLM_API_KEY` | API key for LLM | - | Yes |
| `LLM_BASE_URL` | API endpoint | `https://api.openai.com/v1` | Yes |
| `LLM_MAX_TOKENS` | Max tokens per response | `4096` | No |
| `LLM_TEMPERATURE` | Response creativity | `0.7` | No |

**Supported Providers:**
- `openai` - OpenAI GPT models
- `azure-openai` - Azure OpenAI
- `anthropic` - Claude models
- `deepseek` - DeepSeek models
- `kimi` - Kimi/Moonshot
- `doubao` - Doubao/Volcengine

### 2. Embedding Model

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `EMBEDDING_MODEL` | Embedding model name | `text-embedding-3-small` | Yes |
| `EMBEDDING_API_KEY` | API key (uses LLM_API_KEY if empty) | - | No |
| `EMBEDDING_BASE_URL` | API endpoint (uses LLM_BASE_URL if empty) | `https://api.openai.com/v1` | No |
| `EMBEDDING_DIMENSION` | Vector dimension | `1536` | Yes |

**Embedding Models:**
- `text-embedding-3-small` (1536 dimensions, fast, cheap)
- `text-embedding-3-large` (3072 dimensions, high quality)
- `text-embedding-ada-002` (1536 dimensions, legacy)

### 3. Milvus Vector Database

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MILVUS_HOST` | Milvus server host | `localhost` | Yes |
| `MILVUS_PORT` | Milvus server port | `19530` | Yes |
| `MILVUS_DATABASE` | Database name | `default` | No |
| `MILVUS_USERNAME` | Username | - | No |
| `MILVUS_PASSWORD` | Password | - | No |
| `MILVUS_TIMEOUT` | Connection timeout (ms) | `5000` | No |
| `MILVUS_EMBEDDING_DIMENSION` | Must match embedding dimension | `1536` | Yes |

### 4. MySQL Database

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | MySQL server host | `localhost` | Yes |
| `DB_PORT` | MySQL server port | `3306` | Yes |
| `DB_NAME` | Database name | `rag_db` | Yes |
| `DB_USERNAME` | Database username | - | Yes |
| `DB_PASSWORD` | Database password | - | Yes |

### 5. Redis Cache

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `REDIS_HOST` | Redis server host | `localhost` | No |
| `REDIS_PORT` | Redis server port | `6379` | No |
| `REDIS_PASSWORD` | Redis password | - | No |
| `REDIS_DATABASE` | Database number | `0` | No |
| `REDIS_TIMEOUT` | Connection timeout (ms) | `3000` | No |

### 6. Text Processing

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SPLITTER_CHUNK_SIZE` | Target chunk size (tokens) | `500` | No |
| `SPLITTER_OVERLAP_TOKENS` | Overlap between chunks | `100` | No |
| `SPLITTER_MIN_CHUNK_SIZE` | Minimum chunk size | `50` | No |
| `TOKEN_ENCODING` | Token counting method | `cl100k_base` | No |

### 7. Memory System

| Variable | Description | Default |
|----------|-------------|---------|
| `MEMORY_SESSION_MAX_MESSAGES` | Max messages in session | `20` |
| `MEMORY_SESSION_TTL_MINUTES` | Session TTL (minutes) | `30` |
| `MEMORY_EPISODIC_TOP_K` | Episodic memory retrieval count | `5` |
| `MEMORY_EPISODIC_IMPORTANCE_THRESHOLD` | Importance threshold | `0.3` |
| `MEMORY_PROFILE_UPDATE_FREQUENCY` | Profile update frequency | `5` |

### 8. Multi-tenant

| Variable | Description | Default |
|----------|-------------|---------|
| `TENANT_DEFAULT_ISOLATION_LEVEL` | Isolation level | `METADATA` |
| `TENANT_ENABLE_PARTITION_ISOLATION` | Enable partition isolation | `true` |
| `TENANT_ENABLE_COLLECTION_ISOLATION` | Enable collection isolation | `false` |

**Isolation Levels:**
- `METADATA` - Default, fast, uses metadata filtering
- `PARTITION` - Medium isolation, uses Milvus partitions
- `COLLECTION` - Maximum isolation, separate collections

## Example Configurations

### OpenAI Only
```env
LLM_API_KEY=sk-xxxxx
LLM_MODEL=gpt-4o-mini
EMBEDDING_API_KEY=${LLM_API_KEY}
EMBEDDING_MODEL=text-embedding-3-small
```

### Azure OpenAI
```env
LLM_PROVIDER=azure-openai
LLM_API_KEY=your-azure-key
LLM_BASE_URL=https://your-resource.openai.azure.com
LLM_MODEL=gpt-4o-mini
EMBEDDING_API_KEY=${LLM_API_KEY}
EMBEDDING_BASE_URL=${LLM_BASE_URL}
```

### Zilliz Cloud (Milvus Cloud)
```env
MILVUS_HOST=inxxx.zillizcloud.com
MILVUS_PORT=443
MILVUS_DATABASE=your-database
MILVUS_USERNAME=db_admin
MILVUS_PASSWORD=your-password
```

## Security Notes

1. **Never commit `.env`** - It's in `.gitignore`
2. **Use `.env.example`** as template for documentation
3. **Mask sensitive values** in logs (passwords, API keys)
4. **Use different keys** for production vs development

## Environment Variables Priority

Configuration values are loaded in this order (highest to lowest):

1. System Property (`-Dvar=value`)
2. Environment Variable (`VAR=value`)
3. `.env` file value
4. Default value in `application.yml`
