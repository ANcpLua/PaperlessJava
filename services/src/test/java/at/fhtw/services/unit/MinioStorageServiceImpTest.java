package at.fhtw.services.unit;

import at.fhtw.services.MinioStorageServiceImp;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static at.fhtw.services.unit.TestBase.MinioConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class MinioStorageServiceImpTest {

    private MinioClient minioClient;
    private MinioStorageServiceImp storageService;
    private File downloadedFile;

    static class DummyGetObjectResponse extends GetObjectResponse {
        public DummyGetObjectResponse(String bucket, String object, ByteArrayInputStream stream) {
            super(Headers.of(), bucket, "", object, stream);
        }
    }

    @BeforeEach
    void setUp() {
        minioClient = org.mockito.Mockito.mock(MinioClient.class);
        storageService = new MinioStorageServiceImp(minioClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (downloadedFile != null) {
            Files.deleteIfExists(downloadedFile.toPath());
        }
    }

    @ParameterizedTest(name = "File download with documentId={0} and extension={1}")
    @CsvSource({
            "testDocument, .txt",
            "sampleDocument, .pdf"
    })
    void shouldCreateFileWithCorrectName(String documentId, String extension) throws Exception {
        ByteArrayInputStream simulatedStream = new ByteArrayInputStream(MOCK_CONTENT.getBytes());
        DummyGetObjectResponse simulatedResponse =
                new DummyGetObjectResponse(BUCKET_NAME, documentId, simulatedStream);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(simulatedResponse);
        downloadedFile = storageService.downloadFile(documentId, extension);
        String expectedFileName = documentId + extension;
        assertThat(ASSERT_FILE_EXISTS, downloadedFile.exists(), is(true));
        assertThat(ASSERT_FILE_NAME_MATCHES, downloadedFile.getName(), is(expectedFileName));
    }

    @ParameterizedTest(name = "File content verification with documentId={0} and extension={1}")
    @CsvSource({
            "testDocument, .txt",
            "sampleDocument, .pdf"
    })
    void shouldWriteCorrectContent(String documentId, String extension) throws Exception {
        ByteArrayInputStream simulatedStream = new ByteArrayInputStream(MOCK_CONTENT.getBytes());
        DummyGetObjectResponse simulatedResponse =
                new DummyGetObjectResponse(BUCKET_NAME, documentId, simulatedStream);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(simulatedResponse);
        downloadedFile = storageService.downloadFile(documentId, extension);
        String fileContent = Files.readString(downloadedFile.toPath());
        assertThat(ASSERT_FILE_CONTENT_MATCHES, fileContent, is(MOCK_CONTENT));
    }

    @Test
    @DisplayName("Should propagate MinioClient exceptions")
    void shouldPropagateMinioClientExceptions() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException(MINIO_ERROR_MESSAGE));
        IOException exception = assertThrows(
                IOException.class,
                () -> storageService.downloadFile(DOCUMENT_ID, EXTENSION_TXT)
        );
        assertThat(ASSERT_EXCEPTION_MESSAGE, exception.getMessage(), is(MINIO_ERROR_MESSAGE));
    }
}