package at.fhtw.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Information on RabbitMQ
 * <a href="https://www.rabbitmq.com/documentation.html">RabbitMQ Documentation</a>.
 * </p>
 */

@Slf4j
@Service
public class MessageBrokerImp implements MessageBroker {

    private final RabbitTemplate rabbitTemplate;
    private final String resultQueue;
    private final ObjectMapper mapper;

    public MessageBrokerImp(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.queue.result:document_result_queue}") String resultQueue,
            ObjectMapper mapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.resultQueue = resultQueue;
        this.mapper = mapper;
    }

    @Override
    public void sendToResultQueue(String documentId, String ocrText) {
        try {
            ObjectNode json = mapper.createObjectNode();
            json.put("documentId", documentId);
            json.put("ocrText", ocrText);
            String message = mapper.writeValueAsString(json);
            rabbitTemplate.convertAndSend(resultQueue, message);
        } catch (AmqpException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}