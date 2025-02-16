package at.fhtw.rest.infrastructure;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AppConfig {

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Value("${rabbitmq.queue.processing:document_processing_queue}")
    private String processingQueueName;

    @Value("${rabbitmq.queue.result:document_result_queue}")
    private String resultQueueName;

    @Value("${rabbitmq.exchange:document_exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.processing:document_routing_key}")
    private String processingRoutingKey;

    @Value("${rabbitmq.routing-key.result:document_result_key}")
    private String resultRoutingKey;

    @PostConstruct
    public void logConfig() {
        log.debug("Processing Queue Name: {}", processingQueueName);
        log.debug("Result Queue Name: {}", resultQueueName);
        log.debug("Exchange Name: {}", exchangeName);
        log.debug("Processing Routing Key: {}", processingRoutingKey);
        log.debug("Result Routing Key: {}", resultRoutingKey);
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    @Bean
    public Queue processingQueue() {
        return new Queue(processingQueueName, true);
    }

    @Bean
    public Queue resultQueue() {
        return new Queue(resultQueueName, true);
    }

    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding processingBinding(@Qualifier("processingQueue") Queue processingQueue,
                                     TopicExchange documentExchange) {
        return BindingBuilder.bind(processingQueue).to(documentExchange).with(processingRoutingKey);
    }

    @Bean
    public Binding resultBinding(@Qualifier("resultQueue") Queue resultQueue,
                                 TopicExchange documentExchange) {
        return BindingBuilder.bind(resultQueue).to(documentExchange).with(resultRoutingKey);
    }
}