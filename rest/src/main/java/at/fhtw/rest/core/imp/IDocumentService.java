package at.fhtw.rest.core.imp;

import at.fhtw.rest.api.DocumentRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface IDocumentService {
    DocumentRequest uploadFile(MultipartFile file) throws IOException;
    DocumentRequest renameFile(String docId, String newName) throws IOException;
    byte[] getFileBytes(String docId);
    void deleteDocument(String docId);
    DocumentRequest getDocument(String docId);
    List<DocumentRequest> searchDocuments(String query);
    List<DocumentRequest> getAllDocuments();
}