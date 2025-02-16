package at.fhtw.services.imp;

import java.io.IOException;

public interface IElasticsearchIndexService {
    void indexDocument(
            String documentId,
            String filename,
            String ocrText) throws IOException;
}