package at.fhtw.services;

import at.fhtw.services.imp.IElasticsearchIndexService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ElasticsearchIndexService implements IElasticsearchIndexService {
    private final ElasticsearchClient esClient;
    private final String indexName;

    public ElasticsearchIndexService(ElasticsearchClient esClient, @Value("${elasticsearch.index.name:documents}") String indexName) {
        this.esClient = Objects.requireNonNull(esClient, "ElasticsearchClient cannot be null");
        this.indexName = Objects.requireNonNull(indexName, "Index name cannot be null");
    }

    @Override
    public void indexDocument(String documentId, String filename, String ocrText) throws IOException {
        log.info("[REQUEST] Entering indexDocument with documentId: {}, filename: {}", documentId, filename);
        Map<String, Object> document = createDocumentMap(documentId, filename, ocrText);
        IndexRequest<Map<String, Object>> request = createIndexRequest(documentId, document);
        try {
            IndexResponse response = esClient.index(request);
            logIndexingOutcome(documentId, response);
            log.info("[RESPONSE] Exiting indexDocument for documentId: {}", documentId);
        } catch (IOException e) {
            log.error("[ERROR] indexDocument failed for documentId: {}. Error: {}", documentId, e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, Object> createDocumentMap(String documentId, String filename, String ocrText) {
        Map<String, Object> document = new HashMap<>();
        document.put("documentId", documentId);
        document.put("filename", filename);
        document.put("ocrText", ocrText);
        document.put("@timestamp", Instant.now().toString());
        return document;
    }

    private IndexRequest<Map<String, Object>> createIndexRequest(String documentId, Map<String, Object> document) {
        return IndexRequest.of(builder -> builder.index(indexName).id(documentId).document(document));
    }

    private void logIndexingOutcome(String documentId, IndexResponse response) {
        String action = response.result().name().equalsIgnoreCase("created") ? "indexed" : "updated";
        log.info("[RESPONSE] Document {} successfully with ID: {}", action, documentId);
    }
}