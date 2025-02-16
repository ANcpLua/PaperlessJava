package at.fhtw.services.processor;

import at.fhtw.services.imp.IElasticsearchIndexService;
import at.fhtw.services.imp.IMessageBroker;
import at.fhtw.services.imp.IMinioStorageService;
import at.fhtw.services.imp.IOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessor {
    private final IMinioStorageService storageService;
    private final IOcrService ocrService;
    private final IElasticsearchIndexService indexService;
    private final IMessageBroker messageBroker;

    @RabbitListener(queues = "${rabbitmq.queue.processing}")
    public void processDocument(String message) {
        log.info("[REQUEST] processDocument received message: {}", message);
        File localFile = null;
        try {
            JSONObject json = new JSONObject(message);
            String documentId = json.getString("documentId");
            String filename = json.getString("filename");
            log.info("[REQUEST] Parsed documentId: {} and filename: {}", documentId, filename);
            String fileExtension = ".pdf";
            localFile = storageService.downloadFile(documentId, fileExtension);
            log.info("[RESPONSE] File downloaded for documentId: {}", documentId);
            String extractedText = ocrService.extractText(localFile);
            log.info("[RESPONSE] OCR extraction completed for documentId: {} (text length: {})", documentId, extractedText.length());
            indexService.indexDocument(documentId, filename, extractedText);
            log.info("[RESPONSE] Document indexed for documentId: {}", documentId);
            messageBroker.sendToResultQueue(documentId, extractedText);
            log.info("[RESPONSE] Message sent to result queue for documentId: {}", documentId);
        } catch (Exception e) {
            log.error("[ERROR] processDocument failed. Error: {}", e.getMessage(), e);
        } finally {
            if (localFile != null) {
                if (!localFile.delete()) {
                    log.warn("[ERROR] Could not delete temporary file: {}. Scheduling deletion on exit.", localFile.getAbsolutePath());
                    localFile.deleteOnExit();
                } else {
                    log.info("[RESPONSE] Temporary file {} deleted successfully", localFile.getAbsolutePath());
                }
            }
            log.info("[RESPONSE] processDocument completed");
        }
    }
}