# Enterprise RAG System

<div align="center">

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Milvus](https://img.shields.io/badge/Milvus-2.4.1-orange.svg)](https://milvus.io/)

Enterprise-grade RAG (Retrieval Augmented Generation) system with multi-tenant isolation, long-term memory, and semantic document chunking

[English](./README_EN.md) | 简体中文

</div>

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Core Features](#core-features)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## Features

### Multi-Tenant Isolation
- Three-level isolation strategy: Collection / Partition / Metadata
- Default Metadata-level isolation for data security
- Independent configuration per tenant

### Long-Term Memory
- **Session Memory**: Redis-based real-time session storage
- **Episodic Memory**: Long-term storage of important conversation fragments
- **User Profile**: Automatic user preference learning and updates

### Semantic Chunking
- Intelligent document splitting based on cosine similarity
- Configurable chunk size and overlapping tokens
- 100-token overlap support for context continuity

### Unified Configuration
- Centralized configuration management via `.env` file
- Separate configurations for different services (LLM, Embedding, Milvus, MySQL, Redis)
- Automatic masking of sensitive information

### Enterprise Features

- [x] Multi-tenant data isolation
- [x] Conversation history management
- [x] Document upload and processing
- [x] Vector retrieval
- [x] RAG Q&A
- [x] Long-term memory
- [x] Semantic chunking
- [x] Unified environment configuration
- [x] RESTful API
- [ ] WebSocket real-time chat
- [ ] Frontend interface
- [ ] Authentication and user management

---

## Tech Stack

### Backend

| Technology | Version | Description |
|------------|---------|-------------|
| Java | 21 | Programming language |
| Spring Boot | 3.2.5 | Application framework |
| Spring Data JPA | 3.x | ORM framework |
| Spring Data Redis | 3.x | Cache access |
| Milvus SDK | 2.4.1 | Vector database |
| MySQL | 8.0 | Relational database |
| Redis | - | Cache service |
| dotenv-java | 3.0.2 | Environment variables |
| jtokkit | 0.6.0 | Token counting |

### Frontend (Planned)

| Technology | Version | Description |
|------------|---------|-------------|
| Next.js | 16 | React framework |
| React | 19 | UI library |
| TypeScript | 5 | Type system |
| shadcn/ui | - | UI component library |
| Tailwind CSS | 4 | Styling framework |

---

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Milvus 2.4+ (or Milvus Lite)

### Installation

#### 1. Clone the project

```bash
git clone https://github.com/your-org/enterprise-rag-system.git
cd enterprise-rag-system
```

#### 2. Configure environment variables

```bash
# Copy environment template
cp .env.example .env

# Edit .env file with your services
vim .env
```

#### 3. Start dependency services

```bash
# Start MySQL
service mysql start

# Initialize database
mysql -u root -e "
CREATE DATABASE IF NOT EXISTS rag_db;
CREATE USER IF NOT EXISTS 'rag_user'@'localhost' IDENTIFIED BY 'rag_password';
GRANT ALL PRIVILEGES ON rag_db.* TO 'rag_user'@'localhost';
FLUSH PRIVILEGES;
"

# Start Redis
service redis-server start
```

#### 4. Build the project

```bash
mvn clean package -DskipTests
```

#### 5. Run the application

```bash
java -jar target/rag-system-1.0.0.jar
```

The application will start at `http://localhost:5000`.

### Docker Deployment (Planned)

```bash
docker-compose up -d
```

---

## Configuration

### Environment Variables

All configuration is managed via `.env` file. See [ENVIRONMENT_CONFIG.md](./ENVIRONMENT_CONFIG.md) for details.

#### LLM Configuration

```env
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o-mini
LLM_API_KEY=your_api_key
LLM_BASE_URL=https://api.openai.com/v1
LLM_MAX_TOKENS=4096
LLM_TEMPERATURE=0.7
```

#### Embedding Configuration

```env
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_API_KEY=${LLM_API_KEY}
EMBEDDING_BASE_URL=https://api.openai.com/v1
EMBEDDING_DIMENSION=1536
```

#### Vector Database Configuration

```env
# Milvus (Local)
MILVUS_HOST=localhost
MILVUS_PORT=19530
MILVUS_DATABASE=default

# Or Milvus Cloud
# MILVUS_CLOUD_ENDPOINT=your-endpoint.zillizcloud.com
# MILVUS_CLOUD_TOKEN=your_token
```

#### Relational Database Configuration

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=rag_db
DB_USERNAME=rag_user
DB_PASSWORD=rag_password
```

#### Redis Configuration

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
```

#### Chunking Configuration

```env
SPLITTER_CHUNK_SIZE=500
SPLITTER_OVERLAP_TOKENS=100
SPLITTER_MIN_CHUNK_SIZE=50
```

---

## Project Structure

```
enterprise-rag-system/
├── .env                          # Environment configuration
├── .env.example                  # Environment template
├── .gitignore                    # Git ignore rules
├── pom.xml                       # Maven dependencies
├── README.md                     # Chinese README
├── README_EN.md                  # English README
├── LICENSE                       # MIT License
├── ENVIRONMENT_CONFIG.md         # Configuration guide
├── AGENTS.md                     # Agent development guide
└── src/
    └── main/
        ├── java/com/enterprise/rag/
        │   ├── RagSystemApplication.java
        │   ├── config/                       # Configuration classes
        │   ├── controller/                   # REST controllers
        │   ├── dto/                          # Data transfer objects
        │   ├── entity/                       # Data entities
        │   ├── memory/                       # Memory services
        │   ├── milvus/                       # Vector database
        │   ├── processor/                    # Document processors
        │   ├── repository/                   # Data repositories
        │   ├── service/                      # Business services
        │   └── tenant/                       # Multi-tenant support
        └── resources/
            └── application.yml               # Spring configuration
```

---

## API Documentation

### Chat API

#### Send Message

```http
POST /api/chat
Content-Type: application/json
X-Tenant-ID: your-tenant-id

{
  "message": "Hello, introduce your product",
  "sessionId": "session-123"
}
```

**Response**

```json
{
  "sessionId": "session-123",
  "message": "Hello! Our product is...",
  "timestamp": "2024-01-15T10:30:00Z",
  "contextChunks": 3
}
```

### Document API

#### Upload Document

```http
POST /api/documents/upload
Content-Type: multipart/form-data
X-Tenant-ID: your-tenant-id

file: (binary)
title: "Product Manual"
description: "2024 Product Introduction"
```

#### Get Document List

```http
GET /api/documents
X-Tenant-ID: your-tenant-id
```

### Tenant API

#### Create Tenant

```http
POST /api/tenants
Content-Type: application/json

{
  "name": "Acme Corporation",
  "isolationLevel": "METADATA",
  "description": "Acme Corp dedicated instance"
}
```

---

## Core Features

### Multi-Tenant Isolation

The system supports three tenant isolation levels:

| Level | Description | Use Case |
|-------|-------------|----------|
| `COLLECTION` | Separate Collection per tenant | Highest security requirements |
| `PARTITION` | Separate Partition per tenant | High concurrency scenarios |
| `METADATA` | Isolation via Metadata field (default) | General enterprise use |

### Long-Term Memory Architecture

```
┌─────────────────────────────────────────────────────┐
│                  User Input                          │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│              Session Memory (Redis)                  │
│         Last 20 messages, TTL 30 minutes            │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│            Episodic Memory (MySQL)                  │
│         Top-5 important fragments, threshold > 0.3  │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│              User Profile (MySQL)                   │
│           Update preferences every 5 chats         │
└─────────────────────────────────────────────────────┘
```

### Semantic Chunking Algorithm

```
1. Preprocess text → Sentence segmentation
2. Calculate semantic similarity (cosine) between adjacent sentences
3. If similarity < threshold, start new chunk
4. Otherwise, add sentence to current chunk
5. Add overlapping tokens (default: 100)
6. Repeat until all text is processed
```

---

## Security

### Sensitive Information Protection

- `.env` file is added to `.gitignore` and will not be committed
- API keys and other sensitive info are automatically masked in logs
- For production, consider using environment variables instead of `.env` file

### Tenant Data Isolation

- All database operations carry tenant ID automatically
- Vector database retrieval is scoped to current tenant
- API requests require `X-Tenant-ID` header

---

## Contributing

Contributions are welcome! Please submit PRs or Issues.

### Development Setup

```bash
# Clone repository
git clone https://github.com/your-org/enterprise-rag-system.git

# Install JDK 21
# Reference: https://adoptium.net/

# Install Maven
# Reference: https://maven.apache.org/install.html

# Run tests
mvn test

# Format code (Airbnb Java Style)
mvn spotless:apply
```

### Commit Convention

Please follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: Add new feature
fix: Fix bug
docs: Documentation update
style: Code formatting
refactor: Refactoring
test: Test related
chore: Build/tool related
```

---

## License

This project is open-sourced under MIT License. See [LICENSE](./LICENSE) file for details.

---

## Contact

- GitHub Issues: [https://github.com/your-org/enterprise-rag-system/issues](https://github.com/your-org/enterprise-rag-system/issues)
- Email: your.email@example.com

---

<div align="center">

**If this project helps you, please give it a Star ⭐**

</div>
