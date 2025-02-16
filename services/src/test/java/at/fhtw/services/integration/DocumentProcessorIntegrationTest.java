package at.fhtw.services.integration;

import at.fhtw.services.processor.DocumentProcessor;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static at.fhtw.services.integration.IntegrationTestBase.DocumentConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Container Setup:
 * <ul>
 *   <li><a href="https://www.testcontainers.org/modules/minio/">MinIO Storage</a>
 *     <ul>
 *       <li><a href="https://min.io/docs/minio/container/index.html">MinIO Config Guide</a></li>
 *       <li>Version: RELEASE.2023-09-04T19-57-37Z</li>
 *     </ul>
 *   </li>
 *   <li><a href="https://www.testcontainers.org/modules/elasticsearch/">Elasticsearch</a>
 *     <ul>
 *       <li><a href="https://www.elastic.co/guide/en/elasticsearch/reference/8.17/docker.html">ES Docker Setup</a></li>
 *       <li>Version: 8.17.0</li>
 *     </ul>
 *   </li>
 *   <li><a href="https://www.testcontainers.org/modules/rabbitmq/">RabbitMQ</a>
 *     <ul>
 *       <li><a href="https://www.rabbitmq.com/tutorials">RabbitMQ Tutorials</a></li>
 *       <li>Version: 3.12-management</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * Test Infrastructure:
 * <ul>
 *   <li><a href="https://junit.org/junit5/docs/current/user-guide/#extensions">JUnit Extensions</a></li>
 *   <li><a href="https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers">Spring Boot Testcontainers</a></li>
 *   <li><a href="https://www.testcontainers.org/features/startup_and_waits/">Container Startup & Wait Strategies</a></li>
 *   <li><a href="https://www.testcontainers.org/features/networking/">Shared Network Setup</a></li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(IntegrationTestBase.SharedContainersExtension.class)
public class DocumentProcessorIntegrationTest extends IntegrationTestBase {

    private DocumentProcessor documentProcessor;
    private DummyElasticsearchIndexService dummyIndexService;
    private DummyMessageBroker dummyMessageBroker;
    private DummyMinioStorageService dummyMinioStorageService;

    @BeforeAll
    void setUp() {
        dummyIndexService = new DummyElasticsearchIndexService();
        dummyMessageBroker = new DummyMessageBroker();
        dummyMinioStorageService = new DummyMinioStorageService();
        DummyOcrService dummyOcrService = new DummyOcrService();
        documentProcessor = new DocumentProcessor(
                dummyMinioStorageService,
                dummyOcrService,
                dummyIndexService,
                dummyMessageBroker
        );
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {
        @Test
        @DisplayName("Valid input => Document is indexed, message is sent, and file is deleted")
        void testProcessDocument_happyPath() {
            String validMessage = createMessage(VALID_DOCUMENT_ID, VALID_FILENAME);
            documentProcessor.processDocument(validMessage);
            String indexedText = dummyIndexService.getIndexedText(VALID_DOCUMENT_ID);
            assertThat(indexedText).contains(OCR_EXTRACTED_TEXT_SUFFIX);
            String sentMessage = dummyMessageBroker.getMessage(VALID_DOCUMENT_ID);
            assertThat(sentMessage).contains(OCR_EXTRACTED_TEXT_SUFFIX);
            File lastDownloaded = dummyMinioStorageService.getLastDownloadedFile();
            assertThat(lastDownloaded).doesNotExist();
        }
    }

    @Nested
    @DisplayName("Edge Case & Error Handling Tests")
    class ErrorHandlingTests {
        @Test
        @DisplayName("Missing documentId => No indexing, no broker message")
        void testProcessDocument_missingDocumentId() {
            JSONObject json = new JSONObject();
            json.put(KEY_FILENAME, VALID_FILENAME);
            String invalidMessage = json.toString();
            documentProcessor.processDocument(invalidMessage);
            assertThat(dummyIndexService.getIndexedText(VALID_DOCUMENT_ID)).isNull();
            assertThat(dummyMessageBroker.getMessage(VALID_DOCUMENT_ID)).isNull();
        }

        @Test
        @DisplayName("Missing filename => No indexing, no broker message")
        void testProcessDocument_missingFilename() {
            JSONObject json = new JSONObject();
            json.put(KEY_DOCUMENT_ID, VALID_DOCUMENT_ID);
            String invalidMessage = json.toString();
            documentProcessor.processDocument(invalidMessage);
            assertThat(dummyIndexService.getIndexedText(VALID_DOCUMENT_ID)).isNull();
            assertThat(dummyMessageBroker.getMessage(VALID_DOCUMENT_ID)).isNull();
        }

        @Test
        @DisplayName("Invalid JSON => No indexing, no broker message")
        void testProcessDocument_invalidJson() {
            String invalidMessage = "not valid JSON";
            documentProcessor.processDocument(invalidMessage);
            assertThat(dummyIndexService.getIndexedText(VALID_DOCUMENT_ID)).isNull();
            assertThat(dummyMessageBroker.getMessage(VALID_DOCUMENT_ID)).isNull();
        }

        @Test
        @DisplayName("OCR throws => No indexing, no broker message, file is deleted")
        void testProcessDocument_ocrThrows() {
            dummyMinioStorageService.setShouldThrowDownload(false);
            DummyOcrService.setShouldThrow(true);
            String message = createMessage(DOC_THROW_ID, THROW_FILENAME);
            documentProcessor.processDocument(message);
            assertThat(dummyIndexService.getIndexedText(DOC_THROW_ID)).isNull();
            assertThat(dummyMessageBroker.getMessage(DOC_THROW_ID)).isNull();
            File lastDownloaded = dummyMinioStorageService.getLastDownloadedFile();
            assertThat(lastDownloaded).doesNotExist();
            DummyOcrService.setShouldThrow(false);
        }

        @Test
        @DisplayName("Minio download fails => No OCR, no indexing, no message sent")
        void testProcessDocument_minioDownloadFails() {
            dummyMinioStorageService.setShouldThrowDownload(true);
            String message = createMessage(FAIL_DOCUMENT_ID, FAIL_FILENAME);
            documentProcessor.processDocument(message);
            assertThat(dummyIndexService.getIndexedText(FAIL_DOCUMENT_ID)).isNull();
            assertThat(dummyMessageBroker.getMessage(FAIL_DOCUMENT_ID)).isNull();
            dummyMinioStorageService.setShouldThrowDownload(false);
        }

        @Test
        @DisplayName("Empty OCR text => Document is indexed with empty text, broker is notified")
        void testProcessDocument_emptyOcrText() {
            DummyOcrService.setReturnEmpty(true);
            String message = createMessage(EMPTY_DOCUMENT_ID, EMPTY_FILENAME);
            documentProcessor.processDocument(message);
            String indexedText = dummyIndexService.getIndexedText(EMPTY_DOCUMENT_ID);
            assertThat(indexedText).isEmpty();
            String sentMessage = dummyMessageBroker.getMessage(EMPTY_DOCUMENT_ID);
            assertThat(sentMessage).isEmpty();
            File lastDownloaded = dummyMinioStorageService.getLastDownloadedFile();
            assertThat(lastDownloaded).doesNotExist();
            DummyOcrService.setReturnEmpty(false);
        }
    }
}