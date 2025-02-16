package at.fhtw.services;

import java.io.IOException;

public interface ElasticsearchIndexService {
    void indexDocument(
            String documentId,
            String filename,
            String ocrText) throws IOException;
}