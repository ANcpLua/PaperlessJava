package at.fhtw.rest.message;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Initiates document OCR processing by sending requests to the OCR microservice.
 *
 * <p>
 * Creates JSON messages with document ID and filename, sending them through RabbitMQ
 * to trigger OCR processing. Results are later returned via a separate completion queue.
 * </p>
 *
 * <p>
 * For RabbitMQ details, see:
 * <a href="https://www.rabbitmq.com/documentation.html">RabbitMQ Documentation</a>.
 * </p>
 */

@Slf4j
@Component
@Getter
@Setter
public class ProcessingEventDispatcherImp implements ProcessingEventDispatcher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:document_exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.processing:document_routing_key}")
    private String routingKey;

    public ProcessingEventDispatcherImp(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendProcessingRequest(String docId, String filename) {
        String message = String.format("{\"documentId\":\"%s\",\"filename\":\"%s\"}", docId, filename);
        log.info("[ProcessingEventDispatcherImp.sendProcessingRequest] Sending processing request: {}", message);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        log.info("[ProcessingEventDispatcherImp.sendProcessingRequest] Request sent to exchange '{}' with routing key '{}'", exchangeName, routingKey);
    }
}