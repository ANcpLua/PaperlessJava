package at.fhtw.rest.integration;

import at.fhtw.rest.persistence.MinioStorageServiceImp;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("MinIO Storage Service Integration Tests")
public class MinioStorageServiceImpIntegrationTest {

    private static final Network SHARED_NETWORK = Network.newNetwork();
    private static final String MINIO_VERSION = "RELEASE.2023-09-04T19-57-37Z";

    @Container
    public static final MinIOContainer minioContainer = new MinIOContainer(
            DockerImageName.parse("minio/minio:" + MINIO_VERSION))
            .withNetwork(SHARED_NETWORK)
            .withEnv("MINIO_ROOT_USER", "paperless")
            .withEnv("MINIO_ROOT_PASSWORD", "paperless");

    private MinioStorageServiceImp minioStorageServiceImp;
    private MinioClient minioClient;

    @BeforeAll
    @DisplayName("Initialize MinIO Client and Bucket")
    void setUpAll() {
        String endpoint = String.format("http://%s:%d",
                minioContainer.getHost(),
                minioContainer.getMappedPort(9000));
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials("paperless", "paperless")
                .build();
        minioStorageServiceImp = new MinioStorageServiceImp(minioClient, "documents");
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket("documents").build());
        } catch (Exception e) {
            fail("Failed to create bucket for MinIO tests: " + e.getMessage());
        }
    }

    @AfterAll
    @DisplayName("Cleanup Test Resources")
    void tearDownAll() {
    }

    private MockMultipartFile createTestFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Store and Retrieve File Successfully")
    void testStoreAndLoadFile() throws IOException {
        MockMultipartFile file = createTestFile("test-file.txt", "Hello, MinIO Integration Test!");
        minioStorageServiceImp.storeFile("test-file.txt", file);
        Optional<byte[]> loadedFileBytes = minioStorageServiceImp.loadFile("test-file.txt");
        assertThat(loadedFileBytes).isPresent();
        String loadedContent = new String(loadedFileBytes.get(), StandardCharsets.UTF_8);
        assertThat(loadedContent).isEqualTo("Hello, MinIO Integration Test!");
    }

    @Test
    @DisplayName("Store and Retrieve PDF File Successfully")
    void testStoreAndLoadPdfFile() throws IOException {
        byte[] pdfBytes = "PDF file content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile pdfFile = new MockMultipartFile("file", "test-file.pdf", "application/pdf", pdfBytes);
        minioStorageServiceImp.storeFile("test-file.pdf", pdfFile);
        Optional<byte[]> loadedFileBytes = minioStorageServiceImp.loadFile("test-file.pdf");
        assertThat(loadedFileBytes).isPresent();
        assertThat(loadedFileBytes.get()).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("Delete File Successfully")
    void testDeleteFile() throws IOException {
        MockMultipartFile file = createTestFile("test-delete.txt", "File to be deleted");
        minioStorageServiceImp.storeFile("test-delete.txt", file);
        assertThat(minioStorageServiceImp.loadFile("test-delete.txt")).isPresent();
        minioStorageServiceImp.deleteFile("test-delete.txt");
        assertThat(minioStorageServiceImp.loadFile("test-delete.txt")).isEmpty();
    }

    private static class FailingMultipartFile implements MultipartFile {
        @NotNull
        @Override
        public String getName() {
            return "failing";
        }
        @Override
        public String getOriginalFilename() {
            return "failing.txt";
        }
        @Override
        public String getContentType() {
            return "text/plain";
        }
        @Override
        public boolean isEmpty() {
            return true;
        }
        @Override
        public long getSize() {
            return 0;
        }
        @NotNull
        @Override
        public byte[] getBytes() throws IOException {
            throw new IOException("Failed to store file");
        }
        @NotNull
        @Override
        public java.io.InputStream getInputStream() throws IOException {
            throw new IOException("Failed to store file");
        }
        @Override
        public void transferTo(@NotNull java.io.File dest) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    @Test
    @DisplayName("Handle Storage Failure Gracefully")
    void testStoreFileFailure() {
        MultipartFile failingFile = new FailingMultipartFile();
        assertThatThrownBy(() -> minioStorageServiceImp.storeFile("failing.txt", failingFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to store file");
    }

    @Test
    @DisplayName("Handle File Load When MinIO is Unavailable")
    void testLoadFileWhenMinioUnavailable() {
        MinioClient brokenClient = MinioClient.builder()
                .endpoint("http://127.0.0.1:12345")
                .credentials("paperless", "paperless")
                .build();
        MinioStorageServiceImp brokenStorageService = new MinioStorageServiceImp(brokenClient, "documents");
        Optional<byte[]> result = brokenStorageService.loadFile("any-key");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Dynamically Creates Bucket If Not Exists")
    void testDynamicBucketCreation() throws Exception {
        String dynamicBucketName = "documents-dynamic";
        MinioStorageServiceImp dynamicService = new MinioStorageServiceImp(minioClient, dynamicBucketName);
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(dynamicBucketName).build());
        assertThat(exists).isFalse();
        MockMultipartFile file = createTestFile("dynamic-test.txt", "Test dynamic bucket creation");
        dynamicService.storeFile("dynamic-test.txt", file);
        Thread.sleep(1000);
        exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(dynamicBucketName).build());
        assertThat(exists).isTrue();
        Optional<byte[]> loadedBytes = dynamicService.loadFile("dynamic-test.txt");
        assertThat(loadedBytes).isPresent();
        String content = new String(loadedBytes.get(), StandardCharsets.UTF_8);
        assertThat(content).isEqualTo("Test dynamic bucket creation");
    }
}