package at.fhtw.rest.core.imp;

import java.util.List;

public interface IElasticsearchService {
    void updateFilename(String docId, String newFilename);
    void deleteDocument(String docId);
    List<String> searchIdsByQuery(String query);
}