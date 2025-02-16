package at.fhtw.services.integration;

import at.fhtw.services.imp.IElasticsearchIndexService;
import at.fhtw.services.imp.IMessageBroker;
import at.fhtw.services.imp.IMinioStorageService;
import at.fhtw.services.imp.IOcrService;
import at.fhtw.services.processor.DocumentProcessor;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public abstract class IntegrationTestBase {

    public static class DocumentConstants {
        public static final String KEY_DOCUMENT_ID = "documentId";
        public static final String KEY_FILENAME = "filename";
        public static final String VALID_DOCUMENT_ID = "doc123";
        public static final String VALID_FILENAME = "sample.pdf";
        public static final String OCR_EXTRACTED_TEXT_SUFFIX = " [extracted from dummy content]";
        public static final String DOC_THROW_ID = "docThrow";
        public static final String THROW_FILENAME = "throw.pdf";
        public static final String FAIL_DOCUMENT_ID = "failDoc";
        public static final String FAIL_FILENAME = "fail.pdf";
        public static final String EMPTY_DOCUMENT_ID = "emptyDoc";
        public static final String EMPTY_FILENAME = "empty.pdf";
    }

    public static class ElasticsearchConstants {
        public static final String INDEX_NAME = "documents";
        public static final String FIELD_DOCUMENT_ID = "documentId";
        public static final String FIELD_FILENAME = "filename";
        public static final String FIELD_OCR_TEXT = "ocrText";
        public static final String FIELD_TIMESTAMP = "@timestamp";
        public static final String TEST_DOC_ID_SUCCESS = "test-doc-1";
        public static final String TEST_FILENAME_SUCCESS = "test-file.txt";
        public static final String TEST_OCR_TEXT_SUCCESS = "This is a test document";
        public static final String TEST_DOC_ID_UPDATE = "test-doc-2";
        public static final String ORIGINAL_FILENAME = "original-file.txt";
        public static final String ORIGINAL_OCR_TEXT = "Original OCR text";
        public static final String UPDATED_FILENAME = "updated-file.txt";
        public static final String UPDATED_OCR_TEXT = "Updated OCR text";
        static final String MSG_DOC_FOUND = "Document should be found in Elasticsearch";
        static final String MSG_DOC_SOURCE_NOT_NULL = "Document source should not be null";
        static final String MSG_TIMESTAMP_PRESENT = "Document should contain a timestamp";
        static final String MSG_DOC_FOUND_AFTER_UPDATE = "Document should be found after update";
    }

    public static class MessageBrokerConstants {
        public static final String QUEUE_NAME = "resultQueue";
        public static final int RECEIVE_TIMEOUT = 5000;
        public static final int CONCURRENT_MESSAGE_COUNT = 10;
        public static final int CONCURRENT_THREAD_COUNT = 5;
        public static final String TEST_DOCUMENT_ID = "123";
        public static final String TEST_OCR_TEXT = "Integration Test OCR Text";
    }

    public static class MinioConstants {
        public static final String BUCKET = "documents";
        public static final String TEST_DOCUMENT_ID = "testDocument";
        public static final String NON_EXISTENT_DOCUMENT_ID = "nonExistentDocument";
        public static final String LARGE_DOCUMENT_ID = "largeDocument";
        public static final String TEST_EXTENSION = ".txt";
        public static final String TEST_CONTENT = "Hello World!";
        public static final int LARGE_FILE_SIZE_BYTES = 1024 * 1024;
        public static final String TEST_FILE_PREFIX = "test";
        public static final String LARGE_TEST_FILE_PREFIX = "large_test";
        public static final String NO_SUCH_KEY_ERROR = "NoSuchKey";
        public static final String EXPECTED_ERROR_RESPONSE_EXCEPTION_MSG = "Expected an ErrorResponseException when downloading a non-existent object";
    }

    public static class OcrConstants {
        public static final int DPI = 300;
        public static final String HELLO_PDF_TEXT = "Hello PDF";
        public static final String INVALID_CONTENT = "invalid content";
        public static final String PDF_EXTENSION = ".pdf";
        public static final String PNG_EXTENSION = ".png";
        public static final String UPPERCASE_EXT = ".PDF";
        public static final String TEMP_PDF_PREFIX = "ocrTestPdf";
        public static final String INVALID_PDF_PREFIX = "invalidPdf";
        public static final String UPPERCASE_PDF_PREFIX = "testPdf";
        public static final String EMPTY_PDF_PREFIX = "emptyPdf";
        public static final String IMAGE_FORMAT = "png";
        public static final int IMAGE_WIDTH = 200;
        public static final int IMAGE_HEIGHT = 50;
        public static final float PDF_TEXT_X = 50f;
        public static final float PDF_TEXT_Y = 700f;
        public static final int PDF_FONT_SIZE = 12;
    }

    public static class LoggingConstants {
        public static final String LOG_REQUEST = "[REQUEST] Entering";
        public static final String LOG_RESPONSE = "[RESPONSE] Exiting";
        public static final String LOG_ERROR = "[ERROR]";
        public static final String EXPECTED_REQUEST_MESSAGE = "Expected log message to contain " + LOG_REQUEST;
        public static final String EXPECTED_RESPONSE_MESSAGE = "Expected log message to contain " + LOG_RESPONSE;
        public static final String EXPECTED_ERROR_MESSAGE = "Expected log message to contain " + LOG_ERROR;
        public static final String EXPECTED_EXCEPTION_FAIL_MESSAGE = "Expected RuntimeException to be thrown.";
        public static final String EXPECTED_EXCEPTION_MESSAGE = "Forced error";
        public static final String TEST_INPUT = "testInput";
        public static final String EXPECTED_RESULT = "Hello, " + TEST_INPUT;
    }

    protected String createMessage(String documentId, String filename) {
        JSONObject json = new JSONObject();
        json.put(DocumentConstants.KEY_DOCUMENT_ID, documentId);
        json.put(DocumentConstants.KEY_FILENAME, filename);
        return json.toString();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public DummyMinioStorageService minioStorageService() {
            return new DummyMinioStorageService();
        }

        @Bean
        @Primary
        public DummyOcrService ocrService() {
            return new DummyOcrService();
        }

        @Bean
        @Primary
        public DummyElasticsearchIndexService elasticsearchIndexService() {
            return new DummyElasticsearchIndexService();
        }

        @Bean
        @Primary
        public DummyMessageBroker messageBroker() {
            return new DummyMessageBroker();
        }

        @Bean
        public DocumentProcessor documentProcessor(IMinioStorageService storageService,
                                                   IOcrService ocrService,
                                                   IElasticsearchIndexService indexService,
                                                   IMessageBroker messageBroker) {
            return new DocumentProcessor(storageService, ocrService, indexService, messageBroker);
        }
    }

    public static class DummyMinioStorageService implements IMinioStorageService {

        @Setter
        private boolean shouldThrowDownload;
        @Getter
        private File lastDownloadedFile;

        @Override
        public File downloadFile(String documentId, String fileExtension) throws Exception {
            if (shouldThrowDownload) {
                throw new Exception("Simulated download failure");
            }
            lastDownloadedFile = File.createTempFile("integrationTest-", fileExtension);
            Files.write(lastDownloadedFile.toPath(), "dummy content".getBytes());
            return lastDownloadedFile;
        }
    }

    public static class DummyOcrService implements IOcrService {

        @Setter
        private static boolean shouldThrow = false;
        @Setter
        private static boolean returnEmpty = false;

        @Override
        public String extractText(File file) {
            if (shouldThrow) {
                throw new RuntimeException("Simulated OCR exception");
            }
            if (returnEmpty) {
                return "";
            }
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                return content + DocumentConstants.OCR_EXTRACTED_TEXT_SUFFIX;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class DummyElasticsearchIndexService implements IElasticsearchIndexService {

        private final Map<String, String> indexedText = new HashMap<>();

        @Override
        public void indexDocument(String documentId, String filename, String ocrText) {
            indexedText.put(documentId, ocrText);
        }

        public String getIndexedText(String documentId) {
            return indexedText.get(documentId);
        }
    }

    public static class DummyMessageBroker implements IMessageBroker {

        private final Map<String, String> messages = new HashMap<>();

        @Override
        public void sendToResultQueue(String documentId, String ocrText) {
            messages.put(documentId, ocrText);
        }

        public String getMessage(String documentId) {
            return messages.get(documentId);
        }
    }

    @org.springframework.stereotype.Service
    public static class DummyAspect {

        public String dummyMethod() {
            return LoggingConstants.EXPECTED_RESULT;
        }

        public void dummyErrorMethod() {
            throw new RuntimeException(LoggingConstants.EXPECTED_EXCEPTION_MESSAGE);
        }
    }

    public static class SharedContainersExtension implements BeforeAllCallback, AfterAllCallback {

        private static final Logger logger = LoggerFactory.getLogger(SharedContainersExtension.class);
        private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);
        private static final Network SHARED_NETWORK = Network.newNetwork();
        public static final String MINIO_USERNAME = "minioadmin";
        public static final String MINIO_PASSWORD = "minioadmin";
        private static final String MINIO_VERSION = "RELEASE.2023-09-04T19-57-37Z";
        private static final String RABBITMQ_VERSION = "3.12-management";
        private static final String ELASTICSEARCH_VERSION = "8.17.0";

        public static final MinIOContainer minioContainer = new MinIOContainer(
                DockerImageName.parse("minio/minio:" + MINIO_VERSION))
                .withNetwork(SHARED_NETWORK)
                .withEnv("MINIO_ACCESS_KEY", MINIO_USERNAME)
                .withEnv("MINIO_SECRET_KEY", MINIO_PASSWORD)
                .withStartupTimeout(STARTUP_TIMEOUT);

        public static String getMinioEndpoint() {
            return String.format("http://%s:%d",
                    minioContainer.getHost(),
                    minioContainer.getMappedPort(9000));
        }

        public static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
                DockerImageName.parse("rabbitmq:" + RABBITMQ_VERSION))
                .withNetwork(SHARED_NETWORK)
                .withExposedPorts(5672, 15672)
                .withStartupTimeout(STARTUP_TIMEOUT)
                .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1)
                        .withStartupTimeout(STARTUP_TIMEOUT));

        public static final ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION))
                .withNetwork(SHARED_NETWORK)
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
                .withEnv("ingest.geoip.downloader.enabled", "false")
                .withExposedPorts(9200, 9300);


        static {
            initializeContainers();
        }

        private static void initializeContainers() {
            try {
                logger.info("Initializing test containers...");
                Startables.deepStart(Stream.of(
                        minioContainer,
                        rabbitMQContainer,
                        elasticsearchContainer
                )).join();

                verifyContainerStates();
                setupContainerLogging();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize test containers", e);
            }
        }

        private static void verifyContainerStates() {
            Arrays.asList(minioContainer, rabbitMQContainer, elasticsearchContainer)
                    .forEach(container -> {
                        if (!container.isRunning()) {
                            throw new RuntimeException(String.format("Container %s failed to start", container.getDockerImageName()));
                        }
                    });
        }

        private static void setupContainerLogging() {
            minioContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("MinIO")));
            rabbitMQContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("RabbitMQ")));
            elasticsearchContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("Elasticsearch")));
        }

        @Override
        public void beforeAll(org.junit.jupiter.api.extension.ExtensionContext context) {
            logContainerInfo();
        }

        private static void logContainerInfo() {
            logger.info("""
                            Container Information:
                            RabbitMQ: {}:{}
                            MinIO: {}:{}
                            Elasticsearch: {}
                            """,
                    rabbitMQContainer.getHost(), rabbitMQContainer.getMappedPort(5672),
                    minioContainer.getHost(), minioContainer.getMappedPort(9000),
                    elasticsearchContainer.getHttpHostAddress());
        }

        @Override
        public void afterAll(org.junit.jupiter.api.extension.ExtensionContext context) {
            logger.info("Test containers will be cleaned up automatically");
        }
    }
}