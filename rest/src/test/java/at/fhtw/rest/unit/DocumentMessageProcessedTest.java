package at.fhtw.rest.unit;

import at.fhtw.rest.message.DocumentMessageProcessed;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("DocumentMessageProcessed Unit Tests")
class DocumentMessageProcessedTest {

    private static final String DOC_ID_TEST = "test-doc-123";
    private static final String OCR_TEXT_SAMPLE = "Sample OCR text";
    private static final String DOC_ID = "doc1";
    private static final String OCR_TEXT = "text1";
    private static final String DOC_ID_DIFFERENT = "doc2";
    private static final String OCR_TEXT_DIFFERENT = "text2";
    private static final String DEFAULT_TEXT = "text";
    private static final String VALID_DOC_ID = "valid-id";
    private static final String PROCESSED_AT_LABEL = "processedAt";
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Nested
    @DisplayName("Lombok Generated Methods Tests")
    class LombokGeneratedTests {

        @Test
        @DisplayName("Should test all getters and setters")
        void testGettersAndSetters() {
            DocumentMessageProcessed message = new DocumentMessageProcessed();
            Instant processedAt = Instant.now();
            message.setDocumentId(DOC_ID_TEST);
            message.setOcrText(OCR_TEXT_SAMPLE);
            message.setProcessedAt(processedAt);
            assertThat(message.getDocumentId(), is(DOC_ID_TEST));
            assertThat(message.getOcrText(), is(OCR_TEXT_SAMPLE));
            assertThat(message.getProcessedAt(), is(processedAt));
        }

        @Test
        @DisplayName("Should test equals method with all fields")
        void testEquals() {
            Instant now = Instant.now();
            DocumentMessageProcessed message1 = new DocumentMessageProcessed();
            message1.setDocumentId(DOC_ID);
            message1.setOcrText(OCR_TEXT);
            message1.setProcessedAt(now);
            DocumentMessageProcessed message2 = new DocumentMessageProcessed();
            message2.setDocumentId(DOC_ID);
            message2.setOcrText(OCR_TEXT);
            message2.setProcessedAt(now);
            DocumentMessageProcessed differentMessage = new DocumentMessageProcessed();
            differentMessage.setDocumentId(DOC_ID_DIFFERENT);
            differentMessage.setOcrText(OCR_TEXT_DIFFERENT);
            differentMessage.setProcessedAt(now);
            assertThat(message1, is(message2));
            assertThat(message1, is(not(differentMessage)));
            assertThat(message1, is(not(equalTo(null))));
            assertThat(message1, is(not(equalTo(new Object()))));
        }

        @Test
        @DisplayName("Should test hashCode consistency")
        void testHashCode() {
            Instant now = Instant.now();
            DocumentMessageProcessed message1 = new DocumentMessageProcessed();
            message1.setDocumentId(DOC_ID);
            message1.setOcrText(OCR_TEXT);
            message1.setProcessedAt(now);
            DocumentMessageProcessed message2 = new DocumentMessageProcessed();
            message2.setDocumentId(DOC_ID);
            message2.setOcrText(OCR_TEXT);
            message2.setProcessedAt(now);
            assertThat(message1.hashCode(), is(message2.hashCode()));
            int initialHashCode = message1.hashCode();
            assertThat(message1.hashCode(), is(initialHashCode));
        }

        @Test
        @DisplayName("Should test toString method contains all fields")
        void testToString() {
            DocumentMessageProcessed message = new DocumentMessageProcessed();
            message.setDocumentId(DOC_ID);
            message.setOcrText(OCR_TEXT);
            message.setProcessedAt(Instant.now());
            String toString = message.toString();
            assertThat(toString, containsString(DOC_ID));
            assertThat(toString, containsString(OCR_TEXT));
            assertThat(toString, containsString(PROCESSED_AT_LABEL));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid data")
        void testValidDocument() {
            DocumentMessageProcessed message = new DocumentMessageProcessed();
            message.setDocumentId(VALID_DOC_ID);
            message.setOcrText(DEFAULT_TEXT);
            message.setProcessedAt(Instant.now());
            var violations = validator.validate(message);
            assertThat(violations, empty());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null fields in equals and hashCode")
        void testNullFieldsInEqualsAndHashCode() {
            DocumentMessageProcessed message1 = new DocumentMessageProcessed();
            DocumentMessageProcessed message2 = new DocumentMessageProcessed();
            assertThat(message1, is(message2));
            assertThat(message1.hashCode(), is(message2.hashCode()));
        }

        @Test
        @DisplayName("Should handle null in toString")
        void testNullInToString() {
            DocumentMessageProcessed message = new DocumentMessageProcessed();
            assertDoesNotThrow(message::toString);
            assertThat(message.toString(), notNullValue());
        }
    }
}