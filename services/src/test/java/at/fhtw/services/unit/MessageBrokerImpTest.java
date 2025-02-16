package at.fhtw.services.unit;

import at.fhtw.services.MessageBrokerImp;
import at.fhtw.services.MessageBroker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.Locale;
import java.util.stream.Stream;

import static at.fhtw.services.unit.TestBase.DocumentConstants.VALID_DOCUMENT_ID;
import static at.fhtw.services.unit.TestBase.MessageBrokerConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class MessageBrokerImpTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Captor
    private ArgumentCaptor<String> queueCaptor;
    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private ObjectMapper mapper;
    private MessageBroker messageBroker;

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH);
        mapper = new ObjectMapper();
        MessageBrokerImp concreteBroker = new MessageBrokerImp(rabbitTemplate, QUEUE_NAME, mapper);
        ReflectionTestUtils.setField(concreteBroker, FIELD_RESULT_QUEUE, QUEUE_NAME);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        MethodValidationInterceptor interceptor = new MethodValidationInterceptor(factory.getValidator());
        factory.close();
        ProxyFactory proxyFactory = new ProxyFactory(concreteBroker);
        proxyFactory.addAdvice(interceptor);
        messageBroker = (MessageBroker) proxyFactory.getProxy();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(rabbitTemplate);
    }

    static Stream<Arguments> messagePermutations() {
        return Stream.of(
                Arguments.of(DOC_1, HELLO_WORLD),
                Arguments.of(SPECIAL_CHARS, SPECIAL_CHARS),
                Arguments.of(DOC_WITH_SPACES, TEXT_WITH_SPACES)
        );
    }

    @Nested
    @DisplayName("When sending messages to the result queue")
    class SendingMessagesToResultQueue {
        @Test
        @DisplayName("Given valid document ID and text, message should be sent correctly")
        void givenValidInputs_whenSendingToQueue_thenMessageIsSentCorrectly() throws Exception {
            messageBroker.sendToResultQueue(VALID_DOCUMENT_ID, EXTRACTED_TEXT);
            verify(rabbitTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());
            assertThat(queueCaptor.getValue()).isEqualTo(QUEUE_NAME);
            JsonNode json = mapper.readTree(messageCaptor.getValue());
            assertThat(json.get(JSON_KEY_DOCUMENT_ID).asText()).isEqualTo(VALID_DOCUMENT_ID);
            assertThat(json.get(JSON_KEY_OCR_TEXT).asText()).isEqualTo(EXTRACTED_TEXT);
        }

        @Test
        @DisplayName("Given a very long document ID, message should be sent with full ID")
        void givenLongDocumentId_whenSendingToQueue_thenEntireIdIsSent() throws Exception {
            messageBroker.sendToResultQueue(LONG_DOCUMENT_ID, EXTRACTED_TEXT);
            verify(rabbitTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());
            assertThat(queueCaptor.getValue()).isEqualTo(QUEUE_NAME);
            JsonNode json = mapper.readTree(messageCaptor.getValue());
            assertThat(json.get(JSON_KEY_DOCUMENT_ID).asText()).isEqualTo(LONG_DOCUMENT_ID);
            assertThat(json.get(JSON_KEY_OCR_TEXT).asText()).isEqualTo(EXTRACTED_TEXT);
        }

        @ParameterizedTest
        @MethodSource("at.fhtw.services.unit.MessageBrokerImpTest#messagePermutations")
        @DisplayName("Given various permutations of document ID and text, message is formatted correctly")
        void testSendToResultQueue_withPermutations(String documentId, String ocrText) throws Exception {
            messageBroker.sendToResultQueue(documentId, ocrText);
            verify(rabbitTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());
            assertThat(queueCaptor.getValue()).isEqualTo(QUEUE_NAME);
            JsonNode json = mapper.readTree(messageCaptor.getValue());
            String expectedDocumentId = documentId == null ? "" : documentId.replace("\"", "\\\"");
            String expectedOcrText = ocrText == null ? "" : ocrText.replace("\"", "\\\"");
            assertThat(json.get(JSON_KEY_DOCUMENT_ID).asText()).isEqualTo(expectedDocumentId);
            assertThat(json.get(JSON_KEY_OCR_TEXT).asText()).isEqualTo(expectedOcrText);
        }

        @Test
        @DisplayName("When RabbitTemplate fails, should propagate AmqpException")
        void whenRabbitTemplateFails_thenExceptionIsPropagated() {
            AmqpException simulatedException = new AmqpException(SIMULATED_AMQP_ERROR);
            doThrow(simulatedException).when(rabbitTemplate).convertAndSend(anyString(), anyString());
            AmqpException thrown = catchThrowableOfType(
                    () -> messageBroker.sendToResultQueue(DOC_ID, OCR_TEXT),
                    AmqpException.class
            );
            assertThat(thrown).isNotNull();
            assertThat(thrown.getMessage()).contains(SIMULATED_AMQP_ERROR);
        }

        @Test
        void testSendToResultQueueSuccess() throws JsonProcessingException {
            ObjectMapper mockMapper = mock(ObjectMapper.class);
            MessageBrokerImp localBroker = new MessageBrokerImp(rabbitTemplate, QUEUE_NAME, mockMapper);
            ObjectNode node = mock(ObjectNode.class);
            when(mockMapper.createObjectNode()).thenReturn(node);
            when(mockMapper.writeValueAsString(node)).thenReturn("{\"documentId\":\"" + DOC_ID + "\",\"ocrText\":\"" + OCR_TEXT + "\"}");
            localBroker.sendToResultQueue(DOC_ID, OCR_TEXT);
            verify(rabbitTemplate).convertAndSend(anyString(), anyString());
        }

        @Test
        void testSendToResultQueueThrowsAmqpException() {
            doThrow(new AmqpException(SIMULATED_AMQP_ERROR)).when(rabbitTemplate).convertAndSend(anyString(), anyString());
            AmqpException thrown = catchThrowableOfType(
                    () -> messageBroker.sendToResultQueue(DOC_ID, OCR_TEXT),
                    AmqpException.class
            );
            assertThat(thrown).isNotNull();
            assertThat(thrown.getMessage()).contains(SIMULATED_AMQP_ERROR);
        }

        @Test
        void testSendToResultQueueThrowsRuntimeException() throws JsonProcessingException {
            ObjectMapper mockMapper = mock(ObjectMapper.class);
            MessageBrokerImp localBroker = new MessageBrokerImp(rabbitTemplate, QUEUE_NAME, mockMapper);
            when(mockMapper.createObjectNode()).thenReturn(Mockito.mock(ObjectNode.class));
            when(mockMapper.writeValueAsString(any(ObjectNode.class)))
                    .thenThrow(new JsonProcessingException(SIMULATED_JSON_ERROR) {});
            RuntimeException thrown = catchThrowableOfType(
                    () -> localBroker.sendToResultQueue(DOC_ID, OCR_TEXT),
                    RuntimeException.class
            );
            assertThat(thrown).isNotNull();
            assertThat(thrown.getMessage()).contains(SIMULATED_JSON_ERROR);
        }
    }
}