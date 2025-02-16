package at.fhtw.rest.core;

import java.util.List;

public interface ElasticsearchService {
    void updateFilename(String docId, String newFilename);
    void deleteDocument(String docId);
    List<String> searchIdsByQuery(String query);
}