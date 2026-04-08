Milvus 2.5 引入了极具破坏性创新的 **“Doc-in-Doc-out”（内置 Server 端全文本解析）** 机制。这意味着您**完全不需要**在 Java 代码中手动集成任何中文分词器（如 IK 分词），也不需要手动计算 BM25 的稀疏向量。Milvus 服务端会自动对传入的原始文本进行分词，并将其映射为稀疏向量（Sparse Vector）。

以下是基于 **Spring AI**（调用 Qwen3-Embedding）与 **Milvus 2.5 官方 Java V2 SDK** 实现双路混合检索与 RRF 重排的完整工程级实现。

### 1. 核心依赖清单 (pom.xml)
除了 Spring AI 和通义千问的相关依赖外，请确保引入支持全文检索的 Milvus V2 SDK（需 `>= 2.5.x` 版本）。

```xml
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.5.3</version> </dependency>

<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### 2. 完整代码实现
以下服务组件涵盖了**初始化 Schema（配置内置 BM25 函数）**、**文档入库（仅需传入文本与稠密向量）**以及**双路召回融合**的全生命周期。

```java
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.ranker.RRFRanker;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MilvusDualEngineService {

    private final MilvusClientV2 milvusClient;
    private final EmbeddingModel embeddingModel; // 注入 Spring AI 的通义干问 Embedding 模型
    
    private static final String COLLECTION_NAME = "knowledge_base_qwen";

    public MilvusDualEngineService(MilvusClientV2 milvusClient, EmbeddingModel embeddingModel) {
        this.milvusClient = milvusClient;
        this.embeddingModel = embeddingModel;
        initCollection(); // 启动时初始化
    }

    /**
     * 1. 核心架构初始化：创建包含 BM25 内置函数的 Schema
     */
    private void initCollection() {
        boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                .collectionName(COLLECTION_NAME)
                .build());
        
        if (exists) return;

        // 定义 Schema
        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder().build();

        // 字段1：主键 ID
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(true) // 交给 Milvus 自动生成 ID
                .build());

        // 字段2：原始文本字段 (必须开启 enableAnalyzer)
        schema.addField(AddFieldReq.builder()
                .fieldName("content")
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .enableAnalyzer(true) // 开启 Milvus 服务端分词器
                .build());

        // 字段3：稠密向量 (Qwen3-Embedding-0.6B 为 2048 维)
        schema.addField(AddFieldReq.builder()
                .fieldName("dense_vector")
                .dataType(DataType.FloatVector)
                .dimension(2048)
                .build());

        // 字段4：稀疏向量 (用于 BM25 检索，由 Milvus 自动生成)
        schema.addField(AddFieldReq.builder()
                .fieldName("sparse_vector")
                .dataType(DataType.SparseFloatVector)
                .build());

        // 【关键！】定义 BM25 Function，让 Milvus 自动将 content 转换为 sparse_vector
        schema.addFunction(CreateCollectionReq.Function.builder()
                .name("bm25_text_mapping")
                .functionType(io.milvus.v2.common.FunctionType.BM25)
                .inputFieldNames(Collections.singletonList("content"))
                .outputFieldNames(Collections.singletonList("sparse_vector"))
                .build());

        // 定义索引
        // 稠密向量索引 (COSINE 适用 Qwen3 等多数现代大模型)
        IndexParam denseIndex = IndexParam.builder()
                .fieldName("dense_vector")
                .indexType(IndexParam.IndexType.AUTOINDEX) // 或 HNSW
                .metricType(IndexParam.MetricType.COSINE)
                .build();

        // 稀疏向量索引 (BM25 倒排索引)
        IndexParam sparseIndex = IndexParam.builder()
                .fieldName("sparse_vector")
                .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX)
                .metricType(IndexParam.MetricType.BM25)
                .build();

        // 执行创建
        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(COLLECTION_NAME)
                .collectionSchema(schema)
                .indexParams(List.of(denseIndex, sparseIndex))
                .build());
    }

    /**
     * 2. 数据入库：无需处理稀疏向量，只需存入文本与稠密向量
     */
    public void insertDocument(String text) {
        // 调用 Spring AI + Qwen3 提取稠密向量
        float[] denseEmb = embeddingModel.embed(text);
        
        // 构建 Milvus JSON 数据格式
        JsonObject row = new JsonObject();
        row.addProperty("content", text);
        
        JsonArray vecArray = new JsonArray();
        for (float v : denseEmb) { vecArray.add(v); }
        row.add("dense_vector", vecArray);

        // 注意：无需传入 "sparse_vector"，Milvus 引擎会自动根据 BM25 Function 实时生成
        milvusClient.insert(InsertReq.builder()
                .collectionName(COLLECTION_NAME)
                .data(Collections.singletonList(row))
                .build());
    }

    /**
     * 3. 双引擎检索：执行原生的 Hybrid Search + RRF 融合
     */
    public List<SearchResp.SearchResult> dualEngineSearch(String queryText, int topK) {
        // --- 第一路：语义稠密召回 ---
        // 通过 Spring AI 调用 Qwen3 将查询字符串向量化
        float[] denseEmb = embeddingModel.embed(queryText);
        List<Float> denseList = new ArrayList<>(denseEmb.length);
        for (float v : denseEmb) { denseList.add(v); }

        AnnSearchReq denseReq = AnnSearchReq.builder()
                .vectorFieldName("dense_vector")
                .vectors(Collections.singletonList(new FloatVec(denseList)))
                .metricType(IndexParam.MetricType.COSINE)
                .limit(topK)
                .build();

        // --- 第二路：关键词稀疏召回 ---
        // 【极简实现】直接传入文本包装类 EmbeddedText，Milvus 将在内部使用 BM25 解析并打分
        AnnSearchReq sparseReq = AnnSearchReq.builder()
                .vectorFieldName("sparse_vector")
                .vectors(Collections.singletonList(new EmbeddedText(queryText)))
                .metricType(IndexParam.MetricType.BM25)
                .limit(topK)
                .build();

        // --- 融合阶段：RRF 重排序 ---
        HybridSearchReq hybridReq = HybridSearchReq.builder()
                .collectionName(COLLECTION_NAME)
                .searchRequests(List.of(denseReq, sparseReq))
                .ranker(new RRFRanker(60)) // 常数 K 建议值 60
                .limit(topK)
                .outputFields(List.of("id", "content")) // 明确指定需要返回的字段
                .build();

        // 执行请求并返回
        SearchResp response = milvusClient.hybridSearch(hybridReq);
        if (response.getSearchResults().isEmpty()) {
            return Collections.emptyList();
        }
        return response.getSearchResults().get(0); 
    }
}
```

### 实现亮点与技术突破说明

* **极简入库（Zero-Configuration Sparse Vectors）**：在 `insertDocument()` 方法中，您会发现没有包含任何分词或计算 TF-IDF/BM25 的代码逻辑。因为我们在 Schema 中绑定了 `BM25 Function`，Milvus 服务端在接收到 `content` 数据落盘的瞬间，就在底层完成了分词和稀疏索引倒排树的构建。
* **原生文本检索对象（EmbeddedText）**：在搜索时，不需要再像旧版一样手动计算并投递 `SortedMap<Long, Float>` 结构。V2 SDK 提供的 `EmbeddedText` 允许将用户的原始 Query（如“如何配置环境变量？”）直接当做检索体发给 `sparse_vector` 字段，服务端会自动使用 BM25 算法进行关键词相关性打分。
* **双通道解耦机制**：即便 Spring AI 原生的 `VectorStore` API 目前还不支持复杂的业务侧重排操作，但我们通过注入并直接控制底层 `MilvusClientV2`，依然能优雅地将 Qwen3 语义能力与传统倒排索引优势整合在一起。