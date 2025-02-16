package at.fhtw.services.unit;

import at.fhtw.services.ElasticsearchIndexService;
import at.fhtw.services.MessageBroker;
import at.fhtw.services.MinioStorageService;
import at.fhtw.services.OcrService;
import at.fhtw.services.processor.DocumentProcessor;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static at.fhtw.services.unit.TestBase.DocumentConstants.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class DocumentProcessorTest extends TestBase {

    private final String fileExtension = ".pdf";
    @Mock
    private MinioStorageService mockStorageService;
    @Mock
    private OcrService mockOcrService;
    @Mock
    private ElasticsearchIndexService mockIndexService;
    @Mock
    private MessageBroker mockMessageBroker;

    @TempDir
    Path tempDir;

    private DocumentProcessor documentProcessor;
    private String validMessage;
    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        documentProcessor = new DocumentProcessor(
                mockStorageService,
                mockOcrService,
                mockIndexService,
                mockMessageBroker
        );
        validMessage = createValidDocumentMessage();
        tempFile = Files.createTempFile(tempDir, "doc", ".tmp").toFile();
    }

    @Nested
    @DisplayName("Valid Document Processing")
    class ValidDocumentProcessing {

        @BeforeEach
        void setUpValidProcessing() throws Exception {
            when(mockStorageService.downloadFile(VALID_DOCUMENT_ID, fileExtension)).thenReturn(tempFile);
            when(mockOcrService.extractText(tempFile)).thenReturn(VALID_EXTRACTED_TEXT);
        }

        @Test
        @DisplayName("Should process document in correct order")
        void shouldProcessDocumentInCorrectOrder() throws Exception {
            documentProcessor.processDocument(validMessage);
            InOrder inOrder = inOrder(
                    mockStorageService,
                    mockOcrService,
                    mockIndexService,
                    mockMessageBroker
            );
            inOrder.verify(mockStorageService).downloadFile(VALID_DOCUMENT_ID, fileExtension);
            inOrder.verify(mockOcrService).extractText(tempFile);
            inOrder.verify(mockIndexService).indexDocument(VALID_DOCUMENT_ID, VALID_FILENAME, VALID_EXTRACTED_TEXT);
            inOrder.verify(mockMessageBroker).sendToResultQueue(VALID_DOCUMENT_ID, VALID_EXTRACTED_TEXT);
        }

        @Test
        @DisplayName("Should clean up temporary file after processing")
        void shouldCleanUpTemporaryFile() {
            documentProcessor.processDocument(validMessage);
            assertFileDeleted(tempFile);
        }

        @Test
        @DisplayName("Should handle empty OCR result")
        void shouldHandleEmptyOcrResult() throws Exception {
            when(mockOcrService.extractText(tempFile)).thenReturn("");
            documentProcessor.processDocument(validMessage);
            verify(mockIndexService).indexDocument(VALID_DOCUMENT_ID, VALID_FILENAME, "");
            verify(mockMessageBroker).sendToResultQueue(VALID_DOCUMENT_ID, "");
        }
    }

    @Nested
    @DisplayName("Invalid Input Handling")
    class InvalidInputHandling {

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() {
            String invalidJson = "invalid json";
            documentProcessor.processDocument(invalidJson);
            verifyNoInteractions(mockStorageService, mockOcrService, mockIndexService, mockMessageBroker);
        }

        @Test
        @DisplayName("Should handle missing document ID")
        void shouldHandleMissingDocumentId() {
            JSONObject json = new JSONObject();
            json.put(JSON_KEY_FILENAME, VALID_FILENAME);
            documentProcessor.processDocument(json.toString());
            verifyNoInteractions(mockStorageService, mockOcrService, mockIndexService, mockMessageBroker);
        }

        @Test
        @DisplayName("Should handle missing filename")
        void shouldHandleMissingFilename() {
            JSONObject json = new JSONObject();
            json.put(JSON_KEY_DOCUMENT_ID, VALID_DOCUMENT_ID);
            documentProcessor.processDocument(json.toString());
            verifyNoInteractions(mockStorageService, mockOcrService, mockIndexService, mockMessageBroker);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle storage service failure")
        void shouldHandleStorageServiceFailure() throws Exception {
            when(mockStorageService.downloadFile(VALID_DOCUMENT_ID, fileExtension))
                    .thenThrow(new IOException("Storage service error"));
            documentProcessor.processDocument(validMessage);
            verifyNoInteractions(mockOcrService, mockIndexService, mockMessageBroker);
        }

        @Test
        @DisplayName("Should handle OCR service failure")
        void shouldHandleOcrServiceFailure() throws Exception {
            when(mockStorageService.downloadFile(VALID_DOCUMENT_ID, fileExtension)).thenReturn(tempFile);
            when(mockOcrService.extractText(tempFile)).thenThrow(new RuntimeException("OCR service error"));
            documentProcessor.processDocument(validMessage);
            verifyNoInteractions(mockIndexService, mockMessageBroker);
            assertFileDeleted(tempFile);
        }

        @Test
        @DisplayName("Should handle index service failure")
        void shouldHandleIndexServiceFailure() throws Exception {
            when(mockStorageService.downloadFile(VALID_DOCUMENT_ID, fileExtension)).thenReturn(tempFile);
            when(mockOcrService.extractText(tempFile)).thenReturn(VALID_EXTRACTED_TEXT);
            doThrow(new RuntimeException("Index service error"))
                    .when(mockIndexService).indexDocument(VALID_DOCUMENT_ID, VALID_FILENAME, VALID_EXTRACTED_TEXT);
            documentProcessor.processDocument(validMessage);
            verifyNoInteractions(mockMessageBroker);
            assertFileDeleted(tempFile);
        }

        @Test
        @DisplayName("Should handle message broker failure")
        void shouldHandleMessageBrokerFailure() throws Exception {
            when(mockStorageService.downloadFile(VALID_DOCUMENT_ID, fileExtension)).thenReturn(tempFile);
            when(mockOcrService.extractText(tempFile)).thenReturn(VALID_EXTRACTED_TEXT);
            doThrow(new RuntimeException("Message broker error"))
                    .when(mockMessageBroker).sendToResultQueue(VALID_DOCUMENT_ID, VALID_EXTRACTED_TEXT);
            documentProcessor.processDocument(validMessage);
            verify(mockIndexService).indexDocument(VALID_DOCUMENT_ID, VALID_FILENAME, VALID_EXTRACTED_TEXT);
            assertFileDeleted(tempFile);
        }

        @Test
        @DisplayName("Should handle file deletion failure")
        void shouldHandleFileDeletionFailure() throws Exception {
            File spyFile = spy(tempFile);
            when(mockStorageService.downloadFile(VALID_DOCUMENT_ID, fileExtension)).thenReturn(spyFile);
            when(mockOcrService.extractText(spyFile)).thenReturn(VALID_EXTRACTED_TEXT);
            doReturn(false).when(spyFile).delete();
            documentProcessor.processDocument(validMessage);
            verify(spyFile).deleteOnExit();
            verify(mockIndexService).indexDocument(VALID_DOCUMENT_ID, VALID_FILENAME, VALID_EXTRACTED_TEXT);
            verify(mockMessageBroker).sendToResultQueue(VALID_DOCUMENT_ID, VALID_EXTRACTED_TEXT);
        }
    }
}