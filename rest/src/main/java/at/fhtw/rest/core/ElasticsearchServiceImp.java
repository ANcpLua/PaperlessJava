package at.fhtw.rest.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class ElasticsearchServiceImp implements ElasticsearchService {

    private final ElasticsearchClient esClient;
    private final String indexName;

    public ElasticsearchServiceImp(ElasticsearchClient esClient,
                                   @Value("${elasticsearch.index-name:documents}") String indexName) {
        this.esClient = esClient;
        this.indexName = indexName;
    }

    @Override
    public void updateFilename(String docId, String newFilename) {
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("filename", newFilename);
        updateFields.put("last_modified", Instant.now().toString());
        try {
            esClient.update(
                    UpdateRequest.of(u -> u
                            .index(indexName)
                            .id(docId)
                            .doc(updateFields)
                    ),
                    Map.class
            );
        } catch (IOException e) {
            log.error("Error updating filename for document {}: {}", docId, e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String docId) {
        try {
            esClient.delete(DeleteRequest.of(d -> d.index(indexName).id(docId)));
        } catch (IOException e) {
            log.error("Error deleting document {}: {}", docId, e.getMessage());
        }
    }

    @Override
    public List<String> searchIdsByQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        try {
            var response = esClient.search(
                    s -> s
                            .index(indexName)
                            .allowNoIndices(true)
                            .ignoreUnavailable(true)
                            .query(q -> q.multiMatch(mm -> mm
                                    .fields("filename", "ocrText")
                                    .query(query)
                                    .fuzziness("AUTO")
                            ))
                            .size(50),
                    Map.class
            );
            return response.hits().hits().stream().map(Hit::id).toList();
        } catch (ElasticsearchException | IOException e) {
            log.error("Search failed for query {}: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }
}