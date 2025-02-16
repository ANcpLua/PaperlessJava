package at.fhtw.services.integration;

import at.fhtw.services.MinioStorageService;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static at.fhtw.services.integration.IntegrationTestBase.MinioConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(IntegrationTestBase.SharedContainersExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MinioStorageServiceIntegrationTest extends IntegrationTestBase {

    private MinioClient minioClient;
    private MinioStorageService storageService;

    @BeforeAll
    void setUp() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(SharedContainersExtension.getMinioEndpoint())
                    .credentials(SharedContainersExtension.MINIO_USERNAME, SharedContainersExtension.MINIO_PASSWORD)
                    .build();
            storageService = new MinioStorageService(minioClient);
            initializeBucket();
        } catch (Exception e) {
            throw new RuntimeException(EXPECTED_ERROR_RESPONSE_EXCEPTION_MSG , e);
        }
    }

    @BeforeEach
    void cleanupBucket() {
        try {
            RemoveObjectArgs removeTestDocumentArgs = RemoveObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(TEST_DOCUMENT_ID)
                    .build();
            minioClient.removeObject(removeTestDocumentArgs);

            RemoveObjectArgs removeLargeDocumentArgs = RemoveObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(LARGE_DOCUMENT_ID)
                    .build();
            minioClient.removeObject(removeLargeDocumentArgs);
        } catch (Exception ignored) {}
    }

    private void initializeBucket() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder().bucket(BUCKET).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
    }

    private File createTestFile() throws IOException {
        File tempFile = File.createTempFile(TEST_FILE_PREFIX, TEST_EXTENSION);
        Files.write(tempFile.toPath(), TEST_CONTENT.getBytes());
        return tempFile;
    }

    private File createLargeTestFile() throws IOException {
        byte[] largeData = new byte[LARGE_FILE_SIZE_BYTES];
        Arrays.fill(largeData, (byte) 'A');
        File tempFile = File.createTempFile(LARGE_TEST_FILE_PREFIX, TEST_EXTENSION);
        Files.write(tempFile.toPath(), largeData);
        return tempFile;
    }

    private void uploadTestFile(String objectId, File file) throws Exception {
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(objectId)
                        .filename(file.getAbsolutePath())
                        .build()
        );
    }

    @Test
    @DisplayName("When uploading a file, then it should be available for download")
    void testDownloadFile() throws Exception {
        File uploadFile = createTestFile();
        uploadTestFile(TEST_DOCUMENT_ID, uploadFile);

        try {
            File downloadedFile = storageService.downloadFile(TEST_DOCUMENT_ID, TEST_EXTENSION);
            assertThat(downloadedFile).exists().isReadable();

            String downloadedContent = Files.readString(downloadedFile.toPath());
            assertThat(downloadedContent).isEqualTo(TEST_CONTENT);

            Files.deleteIfExists(downloadedFile.toPath());
        } finally {
            Files.deleteIfExists(uploadFile.toPath());
        }
    }

    @Test
    @DisplayName("When downloading a non-existent file, then an exception should be thrown")
    void testDownloadNonExistentFile() {
        ErrorResponseException exception = assertThrows(ErrorResponseException.class,
                () -> storageService.downloadFile(NON_EXISTENT_DOCUMENT_ID, TEST_EXTENSION));
        assertThat(exception.errorResponse().code()).isEqualTo(NO_SUCH_KEY_ERROR);
    }

    @Test
    @DisplayName("When downloading a large file, then it should be available for download")
    void testDownloadLargeFile() throws Exception {
        File largeUploadFile = createLargeTestFile();
        uploadTestFile(LARGE_DOCUMENT_ID, largeUploadFile);

        try {
            File downloadedFile = storageService.downloadFile(LARGE_DOCUMENT_ID, TEST_EXTENSION);
            assertThat(downloadedFile).exists().isReadable();
            assertThat(downloadedFile.length()).isEqualTo(LARGE_FILE_SIZE_BYTES);
            Files.deleteIfExists(downloadedFile.toPath());
        } finally {
            Files.deleteIfExists(largeUploadFile.toPath());
        }
    }
}