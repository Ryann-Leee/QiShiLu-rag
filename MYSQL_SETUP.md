# MySQL Database Configuration Guide

This guide explains how to configure the RAG system to use MySQL database instead of H2.

## Prerequisites

1. MySQL 8.0 or higher installed
2. MySQL user with appropriate permissions

## Quick Start

### 1. Install MySQL (if not already installed)

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install mysql-server

# Start MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure MySQL installation
sudo mysql_secure_installation
```

### 2. Create Database and User

```sql
-- Connect to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE rag_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user and grant privileges
CREATE USER 'rag_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON rag_db.* TO 'rag_user'@'localhost';
FLUSH PRIVILEGES;

-- Exit
EXIT;
```

### 3. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: rag_user
    password: your_secure_password

  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update tables
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

  # Disable H2 console when using MySQL
  h2:
    console:
      enabled: false
```

Or use profile-based configuration:

```bash
# Run with MySQL profile
java -jar rag-system-1.0.0.jar --spring.profiles.active=mysql
```

### 4. Create Tables (Optional)

JPA will auto-create tables with `ddl-auto: update`. If you prefer manual schema:

```bash
mysql -u rag_user -p rag_db < src/main/resources/schema.sql
```

## Table Structure

### chunk_metadata

Stores metadata for document chunks:

| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(255) | Primary key |
| doc_id | VARCHAR(255) | Document ID (FK) |
| chunk_id | VARCHAR(255) | Unique chunk ID |
| prev_chunk_id | VARCHAR(255) | Previous chunk ID |
| next_chunk_id | VARCHAR(255) | Next chunk ID |
| token_count | INT | Token count |
| page_range | VARCHAR(50) | Source page range |
| content | TEXT | Chunk content |
| chunk_index | INT | Index in document |
| tenant_id | VARCHAR(255) | Tenant ID (FK) |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

### Relationships

- `chunk_metadata.doc_id` → `knowledge_document.id`
- `chunk_metadata.tenant_id` → `tenant.id`

## Verify Setup

### Check connection

```bash
# Test MySQL connection
mysql -u rag_user -p rag_db -e "SELECT 1;"
```

### Check tables

```sql
USE rag_db;
SHOW TABLES;
DESCRIBE chunk_metadata;
```

### Query chunk metadata

```sql
SELECT
    c.id,
    c.doc_id,
    c.chunk_id,
    c.chunk_index,
    c.token_count,
    c.page_range,
    d.file_name
FROM chunk_metadata c
JOIN knowledge_document d ON c.doc_id = d.id
WHERE c.tenant_id = 'your-tenant-id'
ORDER BY c.chunk_index;
```

## Troubleshooting

### Connection refused

```
Check MySQL is running:
sudo systemctl status mysql

Check MySQL is listening:
sudo netstat -tlnp | grep 3306
```

### Authentication failed

```
Reset user password:
ALTER USER 'rag_user'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

### Table creation errors

```
Check user permissions:
SHOW GRANTS FOR 'rag_user'@'localhost';

Grant all privileges:
GRANT ALL PRIVILEGES ON rag_db.* TO 'rag_user'@'localhost';
```

## Performance Optimization

### Connection Pool (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Indexes

Tables have indexes on:
- `doc_id` (foreign key)
- `tenant_id` (foreign key, multi-tenancy)
- `chunk_index` (ordering)

### Query Tips

```sql
-- Use index on doc_id
SELECT * FROM chunk_metadata WHERE doc_id = 'xxx';

-- Use index on tenant_id
SELECT * FROM chunk_metadata WHERE tenant_id = 'xxx';

-- Avoid full table scans
-- Bad: SELECT * FROM chunk_metadata WHERE content LIKE '%search%';
-- Good: Use Milvus for full-text search
```

## Backup and Restore

### Backup

```bash
mysqldump -u rag_user -p rag_db > backup_$(date +%Y%m%d).sql
```

### Restore

```bash
mysql -u rag_user -p rag_db < backup_20240405.sql
```
