package com.enterprise.rag.processor;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.enterprise.rag.entity.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.util.*;

/**
 * Semantic Text Splitter with Token Overlap
 * 
 * Implements intelligent document splitting based on:
 * 1. Natural sentence boundaries
 * 2. Semantic similarity between sentences
 * 3. Configurable token overlap for context preservation
 * 
 * @author Enterprise RAG System
 */
@Slf4j
@Component
public class SemanticOverlapTextSplitter {

    @Value("${embedding.dimension:1536}")
    private int embeddingDimension;

    @Value("${splitter.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${splitter.overlap-tokens:100}")
    private int overlapTokensLimit;

    @Value("${splitter.chunk-size:500}")
    private int chunkSize;

    @Value("${splitter.min-chunk-size:50}")
    private int minChunkSize;

    private final Encoding encoding;

    public SemanticOverlapTextSplitter() {
        // Use CL100K_BASE for token counting (OpenAI compatible)
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * Split document into semantic chunks with overlap
     * 
     * @param document The knowledge document to split
     * @return List of document chunks
     */
    public List<DocumentChunk> splitDocument(KnowledgeDocument document) {
        String text = document.getContent();
        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty document content for document: {}", document.getId());
            return Collections.emptyList();
        }

        // 1. Split text into sentences
        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            log.warn("No sentences found in document: {}", document.getId());
            return Collections.emptyList();
        }

        log.debug("Split document {} into {} sentences", document.getId(), sentences.size());

        // 2. Generate embeddings for each sentence
        List<float[]> embeddings = generateSentenceEmbeddings(sentences);

        // 3. Calculate similarity and identify breakpoints
        List<Integer> breakpoints = calculateBreakpoints(embeddings);
        log.debug("Found {} semantic breakpoints", breakpoints.size());

        // 4. Assemble chunks with overlap
        return assembleChunksWithOverlap(sentences, breakpoints, document);
    }

    /**
     * Split text into sentences using BreakIterator
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
        iterator.setText(text);
        
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    /**
     * Generate embeddings for sentences (placeholder for LLM API)
     * In production, call actual embedding API (OpenAI, etc.)
     */
    private List<float[]> generateSentenceEmbeddings(List<String> sentences) {
        List<float[]> embeddings = new ArrayList<>();
        
        // Placeholder: generate hash-based embeddings for development
        // In production: Use OpenAI text-embedding-3-small or similar
        for (String sentence : sentences) {
            embeddings.add(generatePlaceholderEmbedding(sentence));
        }
        
        return embeddings;
    }

    /**
     * Generate placeholder embedding (hash-based)
     */
    private float[] generatePlaceholderEmbedding(String text) {
        float[] embedding = new float[embeddingDimension];
        if (text != null && !text.isEmpty()) {
            int hash = text.hashCode();
            Random random = new Random(hash);
            for (int i = 0; i < embeddingDimension; i++) {
                embedding[i] = random.nextFloat() * 2 - 1;
            }
        }
        return embedding;
    }

    /**
     * Calculate semantic breakpoints based on similarity threshold
     */
    private List<Integer> calculateBreakpoints(List<float[]> embeddings) {
        List<Integer> breakpoints = new ArrayList<>();
        
        for (int i = 0; i < embeddings.size() - 1; i++) {
            float[] current = embeddings.get(i);
            float[] next = embeddings.get(i + 1);
            
            double similarity = cosineSimilarity(current, next);
            
            // If similarity drops below threshold, treat as semantic boundary
            if (similarity < similarityThreshold) {
                breakpoints.add(i);
                log.trace("Breakpoint at index {} with similarity {}", i, String.format("%.3f", similarity));
            }
        }
        
        return breakpoints;
    }

    /**
     * Assemble chunks with token overlap
     */
    private List<DocumentChunk> assembleChunksWithOverlap(
            List<String> sentences, 
            List<Integer> breakpoints, 
            KnowledgeDocument document) {
        
        List<DocumentChunk> chunks = new ArrayList<>();
        int startIndex = 0;
        List<String> previousChunkCoreSentences = new ArrayList<>();

        for (int i = 0; i <= breakpoints.size(); i++) {
            int endIndex = (i < breakpoints.size()) ? breakpoints.get(i) : sentences.size() - 1;

            List<String> currentChunkSentences = new ArrayList<>();

            // Handle overlap (if not first chunk)
            if (!previousChunkCoreSentences.isEmpty() && overlapTokensLimit > 0) {
                List<String> overlapSentences = collectOverlapSentences(previousChunkCoreSentences);
                currentChunkSentences.addAll(overlapSentences);
            }

            // Extract core sentences for this chunk
            List<String> coreSentences = new ArrayList<>();
            for (int j = startIndex; j <= endIndex; j++) {
                coreSentences.add(sentences.get(j));
            }
            currentChunkSentences.addAll(coreSentences);

            // Create document chunk
            String chunkText = String.join(" ", currentChunkSentences);
            int tokenCount = countTokens(chunkText);

            DocumentChunk chunk = DocumentChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .documentId(document.getId())
                    .tenantId(document.getTenantId())
                    .content(chunkText)
                    .chunkIndex(i)
                    .tokenCount(tokenCount)
                    .sentenceCount(currentChunkSentences.size())
                    .build();

            chunks.add(chunk);

            log.debug("Created chunk {} with {} sentences, {} tokens", 
                    i, currentChunkSentences.size(), tokenCount);

            // Update state for next iteration
            previousChunkCoreSentences = coreSentences;
            startIndex = endIndex + 1;
        }

        return chunks;
    }

    /**
     * Collect overlap sentences from previous chunk (backward traversal)
     */
    private List<String> collectOverlapSentences(List<String> previousSentences) {
        LinkedList<String> overlapSentences = new LinkedList<>();
        int currentOverlapTokens = 0;

        // Traverse backwards
        for (int j = previousSentences.size() - 1; j >= 0; j--) {
            String sentence = previousSentences.get(j);
            int tokenCount = countTokens(sentence);

            // Stop if adding this sentence would exceed limit
            if (currentOverlapTokens + tokenCount > overlapTokensLimit) {
                // Edge case: if no overlap yet, include first sentence to prevent complete context loss
                if (overlapSentences.isEmpty()) {
                    overlapSentences.addFirst(sentence);
                }
                break;
            }

            overlapSentences.addFirst(sentence);
            currentOverlapTokens += tokenCount;
        }

        log.trace("Collected {} overlap sentences ({} tokens)", 
                overlapSentences.size(), currentOverlapTokens);

        return overlapSentences;
    }

    /**
     * Count tokens using CL100K_BASE encoding
     */
    private int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.encode(text).size();
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Document chunk DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class DocumentChunk {
        private String id;
        private String documentId;
        private String tenantId;
        private String content;
        private int chunkIndex;
        private int tokenCount;
        private int sentenceCount;
    }
}
