package com.enterprise.rag.util;

import com.enterprise.rag.entity.ChunkMetadata;
import com.enterprise.rag.repository.ChunkMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chunk Metadata Demo Data Loader
 * 
 * Loads sample chunk metadata for demonstration purposes
 * 
 * @author Enterprise RAG System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkMetadataDemoData implements CommandLineRunner {

    private final ChunkMetadataRepository chunkMetadataRepository;

    @Override
    public void run(String... args) throws Exception {
        // Temporarily disabled to avoid duplicate key errors
        log.info("ChunkMetadataDemoData is temporarily disabled");
    }

    private void loadDemoData() {
        String docId = "demo-doc-001";
        String tenantId = "demo-tenant";

        List<String> chunkIds = new ArrayList<>();

        // First, generate all chunk IDs
        for (int i = 0; i < 5; i++) {
            chunkIds.add(UUID.randomUUID().toString());
        }

        // Create 5 demo chunks and save individually
        for (int i = 0; i < 5; i++) {
            String prevChunkId = (i > 0) ? chunkIds.get(i - 1) : null;
            String nextChunkId = (i < 4) ? chunkIds.get(i + 1) : null;

            int tokenCount = 100 + (i * 50);  // Simulated token count

            ChunkMetadata chunk = ChunkMetadata.builder()
                    .docId(docId)
                    .chunkId(chunkIds.get(i))
                    .prevChunkId(prevChunkId)
                    .nextChunkId(nextChunkId)
                    .tokenCount(tokenCount)
                    .pageRange(i == 0 ? "1" : String.format("%d-%d", i + 1, i + 2))
                    .content(String.format("This is chunk %d of the demo document. " +
                            "It contains sample content for testing the RAG system. " +
                            "The chunk has approximately %d tokens.", i + 1, tokenCount))
                    .chunkIndex(i)
                    .tenantId(tenantId)
                    .build();

            chunkMetadataRepository.save(chunk);
            log.debug("Saved demo chunk {} with chunkId={}", i, chunkIds.get(i));
        }

        log.info("Loaded 5 demo chunk metadata records");
    }
}
