package com.enterprise.rag.controller;

import com.enterprise.rag.dto.FileUploadRequest;
import com.enterprise.rag.service.DocumentIngestionService;
import com.enterprise.rag.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Document upload controller
 * 
 * Handles file uploads with semantic text splitting
 * 
 * @author Enterprise RAG System
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentUploadController {

    private final DocumentIngestionService documentIngestionService;

    /**
     * Upload a document with text content
     * 
     * POST /api/documents/upload
     * Headers: X-Tenant-ID, X-User-ID
     * Body: JSON with fileName, content, contentType
     * 
     * Example:
     * {
     *   "fileName": "document.txt",
     *   "content": "Document content here...",
     *   "contentType": "text/plain"
     * }
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@Valid @RequestBody FileUploadRequest request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        if (tenantId == null || userId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing tenant or user context")
            );
        }

        log.info("Upload request from user {} in tenant {}: {}", 
                userId, tenantId, request.getFileName());

        try {
            DocumentIngestionService.DocumentProcessResult result = 
                    documentIngestionService.processTextDocument(
                            request.getFileName(),
                            request.getContent(),
                            request.getContentType()
                    );

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "documentId", result.getDocumentId(),
                        "fileName", result.getFileName(),
                        "chunkCount", result.getChunkCount(),
                        "totalTokens", result.getTotalTokens(),
                        "message", result.getMessage()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", result.getMessage()
                ));
            }
        } catch (Exception e) {
            log.error("Error processing upload", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Upload a document with base64 encoded content
     * 
     * POST /api/documents/upload/base64
     * 
     * Example:
     * {
     *   "fileName": "document.pdf",
     *   "base64Content": "SGVsbG8gV29ybGQ...",
     *   "contentType": "application/pdf"
     * }
     */
    @PostMapping("/upload/base64")
    public ResponseEntity<?> uploadBase64Document(@Valid @RequestBody Map<String, String> request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        if (tenantId == null || userId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing tenant or user context")
            );
        }

        String fileName = request.get("fileName");
        String base64Content = request.get("base64Content");
        String contentType = request.get("contentType");

        if (fileName == null || base64Content == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing fileName or base64Content")
            );
        }

        log.info("Base64 upload request from user {} in tenant {}: {}", 
                userId, tenantId, fileName);

        try {
            DocumentIngestionService.DocumentProcessResult result = 
                    documentIngestionService.processBase64Document(
                            fileName,
                            base64Content,
                            contentType != null ? contentType : "text/plain"
                    );

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "documentId", result.getDocumentId(),
                        "fileName", result.getFileName(),
                        "chunkCount", result.getChunkCount(),
                        "totalTokens", result.getTotalTokens(),
                        "message", result.getMessage()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", result.getMessage()
                ));
            }
        } catch (Exception e) {
            log.error("Error processing base64 upload", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Get document processing status
     * 
     * GET /api/documents/{documentId}/status
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable String documentId) {
        // TODO: Implement status retrieval
        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "status", "COMPLETED",
                "message", "Document processed successfully"
        ));
    }

    /**
     * Get document chunks
     * 
     * GET /api/documents/{documentId}/chunks
     */
    @GetMapping("/{documentId}/chunks")
    public ResponseEntity<?> getDocumentChunks(@PathVariable String documentId) {
        // TODO: Implement chunk retrieval
        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "chunks", new Object[0],
                "message", "Chunk retrieval not yet implemented"
        ));
    }
}
