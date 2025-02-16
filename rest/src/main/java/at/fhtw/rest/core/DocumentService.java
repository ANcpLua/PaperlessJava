package at.fhtw.rest.core;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.core.imp.IDocumentService;
import at.fhtw.rest.core.imp.IElasticsearchService;
import at.fhtw.rest.infrastructure.mapper.imp.IDocumentMapper;
import at.fhtw.rest.message.imp.IProcessingEventDispatcher;
import at.fhtw.rest.persistence.DocumentEntity;
import at.fhtw.rest.persistence.imp.IDocumentRepository;
import at.fhtw.rest.persistence.imp.IMinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * For more details on the underlying technologies, refer to:
 * <a href="https://min.io/docs/minio/linux/developers/java/minio-java.html" target="_blank">MinIO Documentation</a>,
 * <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html" target="_blank">
 * Elasticsearch Java API Documentation</a>,
 * <a href="https://spring.io/projects/spring-framework" target="_blank">Spring Framework Documentation</a>,
 * and <a href="https://www.rabbitmq.com/docs/queues" target="_blank">RabbitMQ Documentation</a>.
 * </p>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService implements IDocumentService {

    private final IDocumentRepository documentRepository;
    private final IDocumentMapper mapper;
    private final IMinioStorageService minioStorageService;
    private final IProcessingEventDispatcher processingEventDispatcher;
    private final IElasticsearchService elasticsearchService;

    @Override
    public DocumentRequest uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String docId = generateDocId();
        minioStorageService.storeFile(docId, file);
        DocumentEntity entity = new DocumentEntity();
        entity.setId(docId);
        entity.setFilename(file.getOriginalFilename());
        entity.setUploadDate(LocalDateTime.now());
        entity.setFilesize(file.getSize());
        entity.setFiletype(file.getContentType());
        documentRepository.save(entity);
        DocumentRequest request = mapper.toDto(entity);
        processingEventDispatcher.sendProcessingRequest(docId, file.getOriginalFilename());
        return request;
    }

    @Override
    public DocumentRequest renameFile(String docId, String newName) {
        String sanitized = ensurePdfExtension(newName);
        elasticsearchService.updateFilename(docId, sanitized);
        DocumentEntity entity = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        entity.setFilename(sanitized);
        documentRepository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public byte[] getFileBytes(String docId) {
        return minioStorageService.loadFile(docId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + docId));
    }

    @Override
    public void deleteDocument(String docId) {
        minioStorageService.deleteFile(docId);
        documentRepository.deleteById(docId);
        elasticsearchService.deleteDocument(docId);
    }

    @Override
    public List<DocumentRequest> getAllDocuments() {
        return documentRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public DocumentRequest getDocument(String docId) {
        DocumentEntity entity = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        return mapper.toDto(entity);
    }

    @Override
    public List<DocumentRequest> searchDocuments(String query) {
        List<String> docIds = elasticsearchService.searchIdsByQuery(query);
        if (docIds == null) {
            docIds = Collections.emptyList();
        }
        return docIds.stream()
                .map(documentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    private String generateDocId() {
        return UUID.randomUUID().toString();
    }

    public String ensurePdfExtension(String name) {
        String lower = name.toLowerCase();
        while (lower.endsWith(".pdf.pdf")) {
            name = name.substring(0, name.length() - 4);
            lower = name.toLowerCase();
        }
        if (lower.endsWith(".pdf")) {
            return name;
        }
        int dotPos = name.lastIndexOf('.');
        if (dotPos != -1) {
            name = name.substring(0, dotPos);
        }
        return name + ".pdf";
    }
}