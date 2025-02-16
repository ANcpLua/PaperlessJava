package at.fhtw.rest.unit;

import at.fhtw.rest.message.ProcessingEventDispatcherImp;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.*;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class ProcessingEventDispatcherImpTest {

    private RabbitTemplate rabbitTemplate;
    private ProcessingEventDispatcherImp dispatcher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        dispatcher = new ProcessingEventDispatcherImp(rabbitTemplate);
        setField(dispatcher, "exchangeName", "test_exchange");
        setField(dispatcher, "routingKey", "test_routing_key");
        Validation.buildDefaultValidatorFactory();
    }

    @Nested
    @DisplayName("Core Component Initialization Tests")
    class CoreInitializationTests {

        @Test
        @DisplayName("Constructor properly initializes RabbitTemplate")
        void testConstructorInitialization() {
            assertNotNull(dispatcher.getRabbitTemplate(), "RabbitTemplate should be initialized");
            assertEquals(rabbitTemplate, dispatcher.getRabbitTemplate(), "RabbitTemplate should match the injected instance");
        }

        @Test
        @DisplayName("Fields are null before Spring initialization")
        void testFieldsBeforeSpringInitialization() {
            ProcessingEventDispatcherImp newDispatcher = new ProcessingEventDispatcherImp(rabbitTemplate);
            assertNull(newDispatcher.getExchangeName(), "Exchange name should be null before Spring initialization");
            assertNull(newDispatcher.getRoutingKey(), "Routing key should be null before Spring initialization");
        }

        @Test
        @DisplayName("Value annotations set default values correctly")
        void testValueAnnotationDefaults() {
            ProcessingEventDispatcherImp newDispatcher = new ProcessingEventDispatcherImp(rabbitTemplate);
            setField(newDispatcher, "exchangeName", "document_exchange");
            setField(newDispatcher, "routingKey", "document_routing_key");
            assertEquals("document_exchange", newDispatcher.getExchangeName(), "Exchange name should have correct default value");
            assertEquals("document_routing_key", newDispatcher.getRoutingKey(), "Routing key should have correct default value");
        }
    }

    @Nested
    @DisplayName("Field Access and Mutation Tests")
    class FieldAccessTests {

        @Test
        @DisplayName("Exchange name getter and setter work correctly")
        void testExchangeNameGetterSetter() {
            String customExchange = "custom_exchange";
            dispatcher.setExchangeName(customExchange);
            assertEquals(customExchange, dispatcher.getExchangeName(), "Exchange name should be updateable");
        }

        @Test
        @DisplayName("Routing key getter and setter work correctly")
        void testRoutingKeyGetterSetter() {
            String customRoutingKey = "custom_routing_key";
            dispatcher.setRoutingKey(customRoutingKey);
            assertEquals(customRoutingKey, dispatcher.getRoutingKey(), "Routing key should be updateable");
        }
    }

    @Nested
    @DisplayName("Message Processing and Integration Tests")
    class MessageProcessingTests {

        @Test
        @DisplayName("sendProcessingRequest sends correctly formatted message")
        void testSendProcessingRequestValid() {
            String expectedMessage = String.format("{\"documentId\":\"%s\",\"filename\":\"%s\"}", "doc123", "file.pdf");
            dispatcher.sendProcessingRequest("doc123", "file.pdf");
            ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(rabbitTemplate, times(1)).convertAndSend(exchangeCaptor.capture(), routingCaptor.capture(), messageCaptor.capture());
            assertEquals("test_exchange", exchangeCaptor.getValue(), "Exchange name must match configured value");
            assertEquals("test_routing_key", routingCaptor.getValue(), "Routing key must match configured value");
            assertEquals(expectedMessage, messageCaptor.getValue(), "Message must be correctly formatted");
        }

        @Test
        @DisplayName("sendProcessingRequest handles special characters correctly")
        void testSendProcessingRequestWithSpecialCharacters() {
            String expectedMessage = String.format("{\"documentId\":\"%s\",\"filename\":\"%s\"}", "doc-123_@#$", "file name with spaces.pdf");
            dispatcher.sendProcessingRequest("doc-123_@#$", "file name with spaces.pdf");
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), messageCaptor.capture());
            assertEquals(expectedMessage, messageCaptor.getValue(), "Message must be correctly formatted");
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("sendProcessingRequest handles RabbitTemplate exceptions")
        void testSendProcessingRequestHandlesException() {
            doThrow(new RuntimeException("RabbitMQ Error"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
            assertThrows(RuntimeException.class, () -> dispatcher.sendProcessingRequest("doc123", "file.pdf"), "Should propagate RabbitTemplate exceptions");
        }
    }
}