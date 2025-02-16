package at.fhtw.rest.integration;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.core.DocumentService;
import at.fhtw.rest.core.ElasticsearchService;
import at.fhtw.rest.infrastructure.mapper.imp.IDocumentMapper;
import at.fhtw.rest.message.imp.IProcessingEventDispatcher;
import at.fhtw.rest.persistence.DocumentEntity;
import at.fhtw.rest.persistence.imp.IDocumentRepository;
import at.fhtw.rest.persistence.imp.IMinioStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

/**
 * Integration Tests with Multiple Containers
 * Container Modules:
 * <ul>
 *   <li><a href="https://www.testcontainers.org/modules/databases/postgres/">PostgreSQL Container</a></li>
 *   <li><a href="https://www.testcontainers.org/modules/minio/">MinIO Container</a></li>
 *   <li><a href="https://www.testcontainers.org/modules/rabbitmq/">RabbitMQ Container</a></li>
 *   <li><a href="https://www.testcontainers.org/modules/elasticsearch/">Elasticsearch Container</a></li>
 * </ul>
 *
 * Network Setup:
 * <li><a href="https://www.testcontainers.org/features/networking/">Container Network Configuration</a></li>
 *
 * Docker Images:
 * <ul>
 *   <li><a href="https://hub.docker.com/_/postgres">postgres:16-alpine</a></li>
 *   <li><a href="https://hub.docker.com/r/minio/minio">minio/minio:RELEASE.2023-09-04T19-57-37Z</a></li>
 *   <li><a href="https://hub.docker.com/_/rabbitmq">rabbitmq:3.12-management</a></li>
 *   <li><a href="https://www.docker.elastic.co/r/elasticsearch">elasticsearch:8.17.0</a></li>
 * </ul>
 */

@SpringBootTest
@DisplayName("Document Service Integration Tests")
class DocumentServiceIntegrationTest {

    private static final String MINIO_VERSION = "RELEASE.2023-09-04T19-57-37Z";
    private static final String RABBITMQ_VERSION = "3.12-management";
    private static final String ELASTICSEARCH_VERSION = "8.17.0";

    private static final String MINIO_USERNAME = "minioadmin";
    private static final String MINIO_PASSWORD = "minioadmin";
    private static final String MINIO_BUCKET_NAME = "documents";

    private static final String TEST_FILE_CONTENT = "Test file content for integration testing";
    private static final String TEST_PDF_FILENAME = "HelloWorld.pdf";
    private static final String SEARCH_TEST_FILENAME = "searchable-document.pdf";

    @Autowired
    private DocumentService documentService;

    @Autowired
    private IDocumentRepository documentRepository;

    @Autowired
    private IMinioStorageService minioStorageService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @SpyBean
    private IProcessingEventDispatcher processingEventDispatcher;

    @Autowired
    private IDocumentMapper documentMapper;

    @Autowired
    private MinioClient minioClient;

    static final Network SHARED_NETWORK = Network.newNetwork();

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static MinIOContainer minio = new MinIOContainer(
            DockerImageName.parse("minio/minio:" + MINIO_VERSION))
            .withNetwork(SHARED_NETWORK)
            .withEnv("MINIO_ROOT_USER", MINIO_USERNAME)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD);

    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:" + RABBITMQ_VERSION))
            .withNetwork(SHARED_NETWORK);

    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION))
            .withNetwork(SHARED_NETWORK);

    @BeforeAll
    static void startContainers() {
        postgres.start();
        minio.start();
        rabbitmq.start();
        elasticsearch.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                String.format("jdbc:postgresql://localhost:%d/%s",
                        postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.access-key", () -> MINIO_USERNAME);
        registry.add("minio.secret-key", () -> MINIO_PASSWORD);
        registry.add("minio.bucket-name", () -> MINIO_BUCKET_NAME);

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    }

    @BeforeEach
    void setUp() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(MINIO_BUCKET_NAME)
                    .build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(MINIO_BUCKET_NAME)
                        .build());
            }
        } catch (Exception e) {
            fail("Failed to set up MinIO bucket: " + e.getMessage());
        }
    }

    @BeforeEach
    void cleanup() {
        try {
            documentRepository.deleteAll();
            minioStorageService.deleteFile(TEST_PDF_FILENAME);
            elasticsearchService.deleteDocument(SEARCH_TEST_FILENAME);
        } catch (Exception e) {
            fail("Failed to clean up resources before test: " + e.getMessage());
        }
    }

    private MockMultipartFile createTestFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_PDF_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Nested
    @DisplayName("File Upload Operations")
    class UploadTests {

        @Test
        @DisplayName("Should upload file and trigger processing")
        void shouldUploadFileAndTriggerProcessing() throws IOException {
            MockMultipartFile file = createTestFile(TEST_PDF_FILENAME, TEST_FILE_CONTENT);
            DocumentRequest response = documentService.uploadFile(file);
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotBlank();
            assertThat(response.getFilename()).isEqualTo(TEST_PDF_FILENAME);
            Optional<DocumentEntity> persistedEntity = documentRepository.findById(response.getId());
            assertThat(persistedEntity).isPresent();
            assertThat(persistedEntity.get().getFilename()).isEqualTo(TEST_PDF_FILENAME);
            verify(processingEventDispatcher).sendProcessingRequest(response.getId(), TEST_PDF_FILENAME);
        }

        @Test
        @DisplayName("Should reject empty file upload")
        void shouldRejectEmptyFile() {
            MockMultipartFile emptyFile = createTestFile("empty.pdf", "");
            assertThatThrownBy(() -> documentService.uploadFile(emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File must not be empty");
        }
    }

    @Nested
    @DisplayName("File Management Operations")
    class FileManagementTests {

        @Test
        @DisplayName("Should rename file across all services")
        void shouldRenameFileAcrossServices() throws IOException {
            MockMultipartFile file = createTestFile("original.pdf", TEST_FILE_CONTENT);
            DocumentRequest uploadResponse = documentService.uploadFile(file);
            String docId = uploadResponse.getId();
            String newFilename = "renamed.pdf";
            DocumentRequest renameResponse = documentService.renameFile(docId, newFilename);
            assertThat(renameResponse.getFilename()).isEqualTo(newFilename);
            Optional<DocumentEntity> persistedEntity = documentRepository.findById(docId);
            assertThat(persistedEntity).isPresent();
            assertThat(persistedEntity.get().getFilename()).isEqualTo(newFilename);
        }

        @Test
        @DisplayName("Should download MinIO file content")
        void shouldDownloadFileContent() throws IOException {
            MockMultipartFile file = createTestFile(TEST_PDF_FILENAME, TEST_FILE_CONTENT);
            DocumentRequest uploadResponse = documentService.uploadFile(file);
            byte[] downloadedContent = documentService.getFileBytes(uploadResponse.getId());
            assertThat(downloadedContent).isNotNull();
            assertThat(new String(downloadedContent, StandardCharsets.UTF_8)).isEqualTo(TEST_FILE_CONTENT);
        }

        @Test
        @DisplayName("Should delete document and clean up all resources")
        void shouldDeleteDocumentAndCleanup() throws IOException {
            MockMultipartFile file = createTestFile(TEST_PDF_FILENAME, TEST_FILE_CONTENT);
            DocumentRequest response = documentService.uploadFile(file);
            String docId = response.getId();
            documentService.deleteDocument(docId);
            assertThat(documentRepository.findById(docId)).isEmpty();
            assertThat(minioStorageService.loadFile(docId)).isEmpty();
            assertThat(elasticsearchService.searchIdsByQuery(TEST_PDF_FILENAME)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Search Operations")
    class SearchTests {

        @Test
        @DisplayName("Should return HelloWorld.pdf when searching for 'Hello'")
        void shouldReturnMatchingDocuments() {
            String docId = "doc1";
            DocumentEntity entity = new DocumentEntity();
            entity.setId(docId);
            entity.setFilename("HelloWorld.pdf");
            entity.setFilesize(2048L);
            entity.setFiletype("pdf");
            entity.setUploadDate(LocalDateTime.now());
            entity.setOcrJobDone(true);
            entity.setOcrText("Integration testing document");
            documentRepository.save(entity);
            List<String> docIds = elasticsearchService.searchIdsByQuery("Hello");
            assertThat(docIds).isNotNull();
            List<DocumentRequest> expectedResults = docIds.stream()
                    .map(documentRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(documentMapper::toDto)
                    .collect(Collectors.toList());
            List<DocumentRequest> actualResults = documentService.searchDocuments("Hello");
            assertThat(actualResults).isEqualTo(expectedResults);
        }

        @Test
        @DisplayName("Should return empty list for non-matching query")
        void shouldReturnEmptyListForNonMatch() {
            List<DocumentRequest> results = documentService.searchDocuments("nonexistent");
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle non-existent document retrieval")
        void shouldHandleNonExistentDocument() {
            assertThatThrownBy(() -> documentService.getDocument("nonexistent-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Document not found");
        }

        @Test
        @DisplayName("Should handle non-existent file download")
        void shouldHandleNonExistentFileDownload() {
            assertThatThrownBy(() -> documentService.getFileBytes("nonexistent-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File not found");
        }

        @Test
        @DisplayName("Should handle deletion of non-existent document")
        void shouldHandleNonExistentDocumentDeletion() {
            assertThatCode(() -> documentService.deleteDocument("nonexistent-id"))
                    .doesNotThrowAnyException();
        }
    }
}