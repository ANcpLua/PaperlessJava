package at.fhtw.rest.integration;

import at.fhtw.rest.infrastructure.AppConfig;
import at.fhtw.rest.message.ProcessingEventDispatcher;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {AppConfig.class, ProcessingEventDispatcher.class})
public class AppConfigIntegrationTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private Queue processingQueue;

    @Autowired
    private Queue resultQueue;

    @Autowired
    private TopicExchange documentExchange;

    @Autowired
    private Binding processingBinding;

    @Autowired
    private Binding resultBinding;

    @Autowired
    private ProcessingEventDispatcher dispatcher;


    @Test
    @DisplayName("MinioClient bean is created and configured")
    public void testMinioClientBean() {
        assertThat(minioClient).as("MinioClient bean should not be null").isNotNull();
        assertThat(minioClient).as("minioClient should be an instance of MinioClient").isInstanceOf(MinioClient.class);
    }

    @Test
    @DisplayName("ProcessingQueue bean is created with correct properties")
    public void testProcessingQueueBean() {
        assertThat(processingQueue).isNotNull();
        assertThat(processingQueue.getName())
                .as("ProcessingQueue name should match property")
                .isEqualTo("document_processing_queue");
    }

    @Test
    @DisplayName("ResultQueue bean is created with correct properties")
    public void testResultQueueBean() {
        assertThat(resultQueue).isNotNull();
        assertThat(resultQueue.getName())
                .as("ResultQueue name should match property")
                .isEqualTo("document_result_queue");
    }

    @Test
    @DisplayName("DocumentExchange bean is created with correct name")
    public void testDocumentExchangeBean() {
        assertThat(documentExchange).isNotNull();
        assertThat(documentExchange.getName())
                .as("DocumentExchange name should match property")
                .isEqualTo("document_exchange");
    }

    @Test
    @DisplayName("ProcessingBinding bean is created with correct routing configuration")
    public void testProcessingBindingBean() {
        assertThat(processingBinding).isNotNull();
        assertThat(processingBinding.getDestination())
                .as("ProcessingBinding destination should match processing queue name")
                .isEqualTo("document_processing_queue");
        assertThat(processingBinding.getExchange())
                .as("ProcessingBinding exchange should match documentExchange name")
                .isEqualTo("document_exchange");
        assertThat(processingBinding.getRoutingKey())
                .as("ProcessingBinding routing key should match property")
                .isEqualTo("document_routing_key");
    }

    @Test
    @DisplayName("ResultBinding bean is created with correct routing configuration")
    public void testResultBindingBean() {
        assertThat(resultBinding).isNotNull();
        assertThat(resultBinding.getDestination())
                .as("ResultBinding destination should match result queue name")
                .isEqualTo("document_result_queue");
        assertThat(resultBinding.getExchange())
                .as("ResultBinding exchange should match documentExchange name")
                .isEqualTo("document_exchange");
        assertThat(resultBinding.getRoutingKey())
                .as("ResultBinding routing key should match property")
                .isEqualTo("document_result_key");
    }

    @Test
    @DisplayName("ProcessingEventDispatcher default values are correctly injected")
    public void testProcessingEventDispatcherDefaults() {
        assertThat(dispatcher).isNotNull();
        assertThat(dispatcher.getExchangeName())
                .as("exchangeName should match the default property (document_exchange)")
                .isEqualTo("document_exchange");
        assertThat(dispatcher.getRoutingKey())
                .as("routingKey should match the default property (document_routing_key)")
                .isEqualTo("document_routing_key");
    }

    @Test
    @DisplayName("sendProcessingRequest sends a JSON message to the correct exchange and routing key")
    void testSendProcessingRequestIntegration() {
        String docId = "doc123";
        String filename = "testFile.pdf";

        dispatcher.sendProcessingRequest(docId, filename);

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo("document_exchange");
        assertThat(routingKeyCaptor.getValue()).isEqualTo("document_routing_key");
        assertThat(messageCaptor.getValue())
                .contains("\"documentId\":\"doc123\"")
                .contains("\"filename\":\"testFile.pdf\"");
    }
}