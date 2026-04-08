# JDK 21 安装指南

## 问题描述

当前环境缺少 JDK 21 的编译器（javac），导致无法编译 Java 项目。

## 快速安装

```bash
# 方法 1: 使用 apt-get（推荐）
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk

# 方法 2: 如果 apt-get 源中没有 JDK 21，使用 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.10-tem

# 验证安装
java -version
javac -version
```

## 安装后编译

```bash
cd /workspace/projects/rag-system
mvn clean compile
```

## 如果安装失败

如果无法安装 JDK 21，可以使用以下替代方案：

### 方案 1: 降低 Java 版本到 17

修改 `pom.xml`:

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

### 方案 2: 使用 Docker

```bash
docker run -it --rm \
  -v $(pwd):/workspace \
  -w /workspace \
  openjdk:21 \
  mvn clean package
```

## 当前代码状态

✅ 已完成代码编写：
- ChunkMetadata 实体类
- ChunkMetadataRepository 接口
- DocumentIngestionService 更新（自动保存元数据）
- ChunkMetadataController API 端点
- MySQL 配置文件
- 数据库表结构脚本
- 总结文档

⚠️ 需要安装 JDK 21 才能编译运行：
```bash
sudo apt-get install -y openjdk-21-jdk
```
