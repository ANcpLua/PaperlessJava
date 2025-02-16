package at.fhtw.rest.unit;

import at.fhtw.rest.message.CompletionEventHandler;
import at.fhtw.rest.persistence.DocumentEntity;
import at.fhtw.rest.persistence.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class CompletionEventHandlerTest {

    private static final String DOC_ID_VALID = "doc1";
    private static final String DOC_ID_NONEXISTENT = "nonexistent";
    private static final String DOC_ID_BLANK = "";
    private static final String OCR_TEXT = "OCR result";
    private static final String INVALID_JSON = "this is not a valid json";
    private static final String SIMULATED_EXCEPTION_MESSAGE = "Simulated exception";
    private static final String ASSERTION_MSG_OCR_JOB_DONE_AFTER_PROCESSING = "OCR job done flag should be true after processing";
    private static final String ASSERTION_MSG_OCR_TEXT_UPDATED_AFTER_PROCESSING = "OCR text should be updated with the value from the message";
    private static final String ASSERTION_MSG_OCR_JOB_DONE_AFTER_SAVE_EXCEPTION = "OCR job done flag should be set to true even if save fails";
    private static final String ASSERTION_MSG_OCR_TEXT_UPDATED_AFTER_SAVE_EXCEPTION = "OCR text should be updated even if save fails";

    @Mock
    private DocumentRepository documentRepository;

    private ObjectMapper objectMapper;
    private CompletionEventHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new CompletionEventHandler(documentRepository, objectMapper);
    }

    @Setter
    @Getter
    public static class DocumentMessageProcessed {
        private String documentId;
        private String ocrText;

        public DocumentMessageProcessed(String documentId, String ocrText) {
            this.documentId = documentId;
            this.ocrText = ocrText;
        }
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("handleCompletion - valid message updates document")
        void testHandleCompletionValidMessage() throws Exception {
            DocumentMessageProcessed messageDto = new DocumentMessageProcessed(DOC_ID_VALID, OCR_TEXT);
            String messageJson = objectMapper.writeValueAsString(messageDto);
            DocumentEntity entity = new DocumentEntity();
            entity.setId(DOC_ID_VALID);
            entity.setOcrJobDone(false);
            entity.setOcrText("");
            when(documentRepository.findById(DOC_ID_VALID)).thenReturn(Optional.of(entity));
            handler.handleCompletion(messageJson);
            verify(documentRepository, times(1)).findById(DOC_ID_VALID);
            verify(documentRepository, times(1)).save(entity);
            assertThat(entity.isOcrJobDone()).as(ASSERTION_MSG_OCR_JOB_DONE_AFTER_PROCESSING).isTrue();
            assertThat(entity.getOcrText()).as(ASSERTION_MSG_OCR_TEXT_UPDATED_AFTER_PROCESSING).isEqualTo(OCR_TEXT);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("handleCompletion - blank documentId causes exception and no repository calls")
        void testHandleCompletionBlankDocumentId() throws Exception {
            DocumentMessageProcessed messageDto = new DocumentMessageProcessed(DOC_ID_BLANK, OCR_TEXT);
            String messageJson = objectMapper.writeValueAsString(messageDto);
            handler.handleCompletion(messageJson);
            verify(documentRepository, never()).findById(any());
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("handleCompletion - document not found leads to no update")
        void testHandleCompletionDocumentNotFound() throws Exception {
            DocumentMessageProcessed messageDto = new DocumentMessageProcessed(DOC_ID_NONEXISTENT, OCR_TEXT);
            String messageJson = objectMapper.writeValueAsString(messageDto);
            when(documentRepository.findById(DOC_ID_NONEXISTENT)).thenReturn(Optional.empty());
            handler.handleCompletion(messageJson);
            verify(documentRepository, times(1)).findById(DOC_ID_NONEXISTENT);
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("handleCompletion - invalid JSON message causes parse error and no repository calls")
        void testHandleCompletionInvalidJson() {
            handler.handleCompletion(INVALID_JSON);
            verify(documentRepository, never()).findById(any());
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("handleCompletion - repository save throws exception and is caught")
        void testHandleCompletionRepositorySaveException() throws Exception {
            DocumentMessageProcessed messageDto = new DocumentMessageProcessed(DOC_ID_VALID, OCR_TEXT);
            String messageJson = objectMapper.writeValueAsString(messageDto);
            DocumentEntity entity = new DocumentEntity();
            entity.setId(DOC_ID_VALID);
            entity.setOcrJobDone(false);
            entity.setOcrText("");
            when(documentRepository.findById(DOC_ID_VALID)).thenReturn(Optional.of(entity));
            doThrow(new RuntimeException(SIMULATED_EXCEPTION_MESSAGE)).when(documentRepository).save(entity);
            handler.handleCompletion(messageJson);
            verify(documentRepository, times(1)).findById(DOC_ID_VALID);
            verify(documentRepository, times(1)).save(entity);
            assertThat(entity.isOcrJobDone()).as(ASSERTION_MSG_OCR_JOB_DONE_AFTER_SAVE_EXCEPTION).isTrue();
            assertThat(entity.getOcrText()).as(ASSERTION_MSG_OCR_TEXT_UPDATED_AFTER_SAVE_EXCEPTION).isEqualTo(OCR_TEXT);
        }

        @Nested
        @DisplayName("Error Handling Tests for Blank/Null Document ID")
        class DocumentIdValidationTests {

            @Test
            @DisplayName("handleCompletion - blank documentId causes no repository calls")
            void testHandleCompletionBlankDocumentId() throws Exception {
                DocumentMessageProcessed messageDto = new DocumentMessageProcessed(DOC_ID_BLANK, OCR_TEXT);
                String messageJson = objectMapper.writeValueAsString(messageDto);
                handler.handleCompletion(messageJson);
                verify(documentRepository, never()).findById(any());
                verify(documentRepository, never()).save(any());
            }

            @Test
            @DisplayName("handleCompletion - null documentId causes no repository calls")
            void testHandleCompletionNullDocumentId() throws Exception {
                DocumentMessageProcessed messageDto = new DocumentMessageProcessed(null, OCR_TEXT);
                String messageJson = objectMapper.writeValueAsString(messageDto);
                handler.handleCompletion(messageJson);
                verify(documentRepository, never()).findById(any());
                verify(documentRepository, never()).save(any());
            }
        }
    }
}