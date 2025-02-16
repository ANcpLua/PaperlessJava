package at.fhtw.rest.api;

import at.fhtw.rest.core.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload a new document")
    @PostMapping
    public ResponseEntity<DocumentRequest> upload(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("Received upload request for file: {}", file.getOriginalFilename());
        DocumentRequest result = documentService.uploadFile(file);
        log.info("Upload successful. Document ID: {}, File Name: {}", result.getId(), result.getFilename());
        return ResponseEntity.status(201).body(result);
    }

    @Operation(summary = "Get all documents")
    @GetMapping
    public ResponseEntity<List<DocumentRequest>> listAllDocuments() {
        List<DocumentRequest> docs = documentService.getAllDocuments();
        return ResponseEntity.ok(docs);
    }

    @Operation(summary = "Rename document")
    @PatchMapping("/{id}")
    public ResponseEntity<DocumentRequest> rename(
            @PathVariable("id") @NotBlank(message = "Document ID must not be blank") String id,
            @RequestParam("newName") @NotBlank(message = "New name must not be blank") String newName
    ) throws IOException {
        log.info("Received rename request for document ID: {} with new name: {}", id, newName);
        DocumentRequest updated = documentService.renameFile(id, newName);
        log.info("Rename successful. Document ID: {}, New Name: {}", updated.getId(), updated.getFilename());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Download document")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable("id") @NotBlank(message = "Document ID must not be blank") String id
    ) {
        log.info("Received download request for document ID: {}", id);
        byte[] data = documentService.getFileBytes(id);
        log.info("Download successful for document ID: {} ({} bytes)", id, data.length);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(data);
    }

    @Operation(summary = "Delete document")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") @NotBlank(message = "Document ID must not be blank") String id
    ) {
        log.info("Received delete request for document ID: {}", id);
        documentService.deleteDocument(id);
        log.info("Delete successful for document ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get document details")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentRequest> getDocument(
            @PathVariable("id") @NotBlank(message = "Document ID must not be blank") String id
    ) {
        log.info("Received request for document details with ID: {}", id);
        DocumentRequest doc = documentService.getDocument(id);
        log.info("Document details retrieved for ID: {}", id);
        return ResponseEntity.ok(doc);
    }

    @Operation(summary = "Search documents")
    @GetMapping("/search")
    public ResponseEntity<List<DocumentRequest>> search(
            @RequestParam(value = "query", required = false) String query
    ) {
        log.info("Received search request with query: '{}'", query);

        if (query == null || query.isBlank()) {
            log.info("Empty query provided. Please enter a document name or OCR text to search.");
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<DocumentRequest> results = documentService.searchDocuments(query);
        log.info("Search completed. Query: '{}', Results count: {}", query, results.size());
        return ResponseEntity.ok(results);
    }
}