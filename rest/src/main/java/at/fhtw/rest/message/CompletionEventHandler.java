package at.fhtw.rest.message;

import at.fhtw.rest.persistence.imp.IDocumentRepository;
import at.fhtw.rest.persistence.DocumentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Handles OCR completion messages from the OCR microservice via RabbitMQ.
 *
 * <p>
 * Listens for completion events, deserializes them into {@link DocumentMessageProcessed} DTOs,
 * and updates the corresponding {@link DocumentEntity} in the repository with the OCR results.
 * </p>
 *
 * <p>
 * For RabbitMQ messaging details, see:
 * <a href="https://docs.spring.io/spring-amqp/docs/current/api/">Spring AMQP Documentation</a>.
 * </p>
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class CompletionEventHandler {
    private final IDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.result:document_result_queue}")
    public void handleCompletion(String message) {
        log.info("[CompletionEventHandler.handleCompletion] Received message: {}", message);
        try {
            DocumentMessageProcessed dto = objectMapper.readValue(message, DocumentMessageProcessed.class);
            log.info("[CompletionEventHandler.handleCompletion] Parsed DTO for documentId: {}", dto.getDocumentId());
            if (dto.getDocumentId() == null || dto.getDocumentId().isBlank()) {
                log.warn("[CompletionEventHandler.handleCompletion] Document ID is blank in message: {}", message);
                throw new IllegalArgumentException("Document ID in message is blank");
            }
            DocumentEntity entity = documentRepository.findById(dto.getDocumentId())
                    .orElseThrow(() -> {
                        log.warn("[CompletionEventHandler.handleCompletion] Document not found for ID: {}", dto.getDocumentId());
                        return new IllegalArgumentException("Not found: " + dto.getDocumentId());
                    });
            entity.setOcrJobDone(true);
            entity.setOcrText(dto.getOcrText());
            documentRepository.save(entity);
            log.info("[CompletionEventHandler.handleCompletion] Successfully processed completion event for documentId: {}", dto.getDocumentId());
        } catch (Exception e) {
            log.error("[CompletionEventHandler.handleCompletion] Failed to process completion event: {}", e.getMessage(), e);
        }
    }
}