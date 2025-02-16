package at.fhtw.services.integration;

import at.fhtw.services.MessageBroker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static at.fhtw.services.integration.IntegrationTestBase.DocumentConstants.KEY_DOCUMENT_ID;
import static at.fhtw.services.integration.IntegrationTestBase.ElasticsearchConstants.FIELD_OCR_TEXT;
import static at.fhtw.services.integration.IntegrationTestBase.MessageBrokerConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(IntegrationTestBase.SharedContainersExtension.class)
@DisplayName("MessageBroker Integration Tests")
class MessageBrokerIntegrationTest extends IntegrationTestBase {

    private RabbitTemplate rabbitTemplate;
    private MessageBroker messageBroker;
    private ObjectMapper objectMapper;
    private CachingConnectionFactory connectionFactory;

    @BeforeEach
    void setup() {
        String rabbitHost = SharedContainersExtension.rabbitMQContainer.getHost();
        int rabbitPort = SharedContainersExtension.rabbitMQContainer.getMappedPort(5672);

        connectionFactory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        new RabbitAdmin(connectionFactory).declareQueue(new Queue(QUEUE_NAME, false));

        messageBroker = new MessageBroker(rabbitTemplate, QUEUE_NAME, new ObjectMapper());
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void cleanup() {
        while (true) {
            if (rabbitTemplate.receiveAndConvert(QUEUE_NAME, 100) == null) break;
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("Given a message broker, when sending a message to the result queue, then the message should be received")
    void testSendToResultQueue() throws Exception {
        messageBroker.sendToResultQueue(TEST_DOCUMENT_ID, TEST_OCR_TEXT);
        String received = (String) rabbitTemplate.receiveAndConvert(QUEUE_NAME, RECEIVE_TIMEOUT);
        assertThat(received).isNotNull();

        JsonNode jsonNode = objectMapper.readTree(received);
        assertThat(jsonNode.get(KEY_DOCUMENT_ID).asText()).isEqualTo(TEST_DOCUMENT_ID);
        assertThat(jsonNode.get(FIELD_OCR_TEXT).asText()).isEqualTo(TEST_OCR_TEXT);
    }

    @Test
    @DisplayName("Given a message broker, when sending multiple messages concurrently, then all messages should be received")
    void testConcurrentSending() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_MESSAGE_COUNT);

        try {
            for (int i = 0; i < CONCURRENT_MESSAGE_COUNT; i++) {
                final String id = String.valueOf(i);
                executorService.submit(() -> {
                    messageBroker.sendToResultQueue(id, "text" + id);
                    latch.countDown();
                });
            }
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

            int receivedCount = 0;
            for (int i = 0; i < CONCURRENT_MESSAGE_COUNT; i++) {
                String received = (String) rabbitTemplate.receiveAndConvert(QUEUE_NAME, RECEIVE_TIMEOUT);
                if (received != null) {
                    receivedCount++;
                    JsonNode jsonNode = objectMapper.readTree(received);
                    assertThat(jsonNode.has(KEY_DOCUMENT_ID)).isTrue();
                    assertThat(jsonNode.has(FIELD_OCR_TEXT)).isTrue();
                }
            }
            assertThat(receivedCount).isEqualTo(CONCURRENT_MESSAGE_COUNT);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("Given a message broker, when sending a large message, then the message should be received")
    void testLargeMessage() throws Exception {
        String largeText = "A".repeat(100_000);
        messageBroker.sendToResultQueue(TEST_DOCUMENT_ID, largeText);
        String received = (String) rabbitTemplate.receiveAndConvert(QUEUE_NAME, RECEIVE_TIMEOUT);
        assertThat(received).isNotNull();

        JsonNode jsonNode = objectMapper.readTree(received);
        assertThat(jsonNode.get(KEY_DOCUMENT_ID).asText()).isEqualTo(TEST_DOCUMENT_ID);
        assertThat(jsonNode.get(FIELD_OCR_TEXT).asText()).isEqualTo(largeText);
    }
}