package at.fhtw.rest.unit;

import at.fhtw.rest.persistence.MinioStorageServiceImp;
import at.fhtw.rest.persistence.MinioStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class MinioStorageServiceImpTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageService minioStorageService;

    @BeforeEach
    void setUp() {
        minioStorageService = new MinioStorageServiceImp(minioClient, "documents");
    }

    @Nested
    class StoreFileTests {

        @Test
        void testStoreFileSuccess() throws Exception {
            when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getSize()).thenReturn(123L);
            when(mockFile.getBytes()).thenReturn("dummy content".getBytes());
            when(mockFile.getContentType()).thenReturn("application/pdf");
            assertThatCode(() -> minioStorageService.storeFile("someObjectKey", mockFile))
                    .doesNotThrowAnyException();
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        void testStoreFileFailure() throws Exception {
            when(minioClient.putObject(any(PutObjectArgs.class)))
                    .thenThrow(new IOException("Simulated IO failure"));
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getSize()).thenReturn(123L);
            when(mockFile.getBytes()).thenReturn("dummy content".getBytes());
            when(mockFile.getContentType()).thenReturn("application/pdf");
            assertThatThrownBy(() -> minioStorageService.storeFile("failKey", mockFile))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("Should use default content type when file content type is null")
        void testStoreFileWithNullContentType() throws Exception {
            when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getSize()).thenReturn(123L);
            when(mockFile.getBytes()).thenReturn("dummy content".getBytes());
            when(mockFile.getContentType()).thenReturn(null);
            ArgumentCaptor<PutObjectArgs> putObjectArgsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
            assertThatCode(() -> minioStorageService.storeFile("testKey", mockFile))
                    .doesNotThrowAnyException();
            verify(minioClient).putObject(putObjectArgsCaptor.capture());
            PutObjectArgs capturedArgs = putObjectArgsCaptor.getValue();
            assertThat(capturedArgs.contentType()).isEqualTo("application/octet-stream");
        }
    }

    @Nested
    class LoadFileTests {

        @Test
        void testLoadFileSuccess() throws Exception {
            GetObjectResponse getObjectResponse = mock(GetObjectResponse.class);
            when(getObjectResponse.readAllBytes()).thenReturn("file data".getBytes());
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);
            Optional<byte[]> result = minioStorageService.loadFile("someObjectKey");
            assertThat(result).isPresent();
            assertThat(new String(result.get())).isEqualTo("file data");
            verify(minioClient).getObject(any(GetObjectArgs.class));
        }

        @Test
        void testLoadFileFailure() throws Exception {
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new IOException("Simulated IO failure"));
            Optional<byte[]> result = minioStorageService.loadFile("failKey");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DeleteFileTests {

        @Test
        void testDeleteFileSuccess() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
            assertThatCode(() -> minioStorageService.deleteFile("someObjectKey"))
                    .doesNotThrowAnyException();
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void testDeleteFileFailure() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            doThrow(new IOException("Simulated delete failure"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));
            assertThatThrownBy(() -> minioStorageService.deleteFile("failKey"))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("Should handle non-existent bucket gracefully and log warning")
        void testDeleteFileWithNonExistentBucket(CapturedOutput output) throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
            assertThatCode(() -> minioStorageService.deleteFile("someObjectKey"))
                    .doesNotThrowAnyException();
            verify(minioClient).bucketExists(any(BucketExistsArgs.class));
            verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
            String logs = output.getOut();
            assertThat(logs).contains("Bucket 'documents' does not exist");
        }
    }

    @Nested
    class RenameFileTests {

        @Test
        @DisplayName("Should successfully delete file when bucket exists")
        void testDeleteFileSuccess() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
            assertThatCode(() -> minioStorageService.deleteFile("someObjectKey"))
                    .doesNotThrowAnyException();
            verify(minioClient).bucketExists(any(BucketExistsArgs.class));
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should handle non-existent bucket gracefully")
        void testDeleteFileWithNonExistentBucket() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
            assertThatCode(() -> minioStorageService.deleteFile("someObjectKey"))
                    .doesNotThrowAnyException();
            verify(minioClient).bucketExists(any(BucketExistsArgs.class));
            verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when delete fails")
        void testDeleteFileFailure() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            doThrow(new RuntimeException("Simulated delete failure"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));
            assertThatThrownBy(() -> minioStorageService.deleteFile("failKey"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should successfully load file")
        void testLoadFileSuccess() throws Exception {
            GetObjectResponse getObjectResponse = mock(GetObjectResponse.class);
            when(getObjectResponse.readAllBytes()).thenReturn("file data".getBytes());
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);
            Optional<byte[]> result = minioStorageService.loadFile("someObjectKey");
            assertThat(result).isPresent();
            assertThat(new String(result.get())).isEqualTo("file data");
            verify(minioClient).getObject(any(GetObjectArgs.class));
        }

        @Test
        @DisplayName("Should return empty Optional when load fails")
        void testLoadFileFailure() throws Exception {
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new IOException("Simulated IO failure"));
            Optional<byte[]> result = minioStorageService.loadFile("failKey");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should successfully store file")
        void testStoreFileSuccess() throws Exception {
            when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getSize()).thenReturn(123L);
            when(mockFile.getBytes()).thenReturn("dummy content".getBytes());
            when(mockFile.getContentType()).thenReturn("application/pdf");
            assertThatCode(() -> minioStorageService.storeFile("someObjectKey", mockFile))
                    .doesNotThrowAnyException();
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw IOException when store fails")
        void testStoreFileFailure() throws Exception {
            when(minioClient.putObject(any(PutObjectArgs.class)))
                    .thenThrow(new IOException("Simulated IO failure"));
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getSize()).thenReturn(123L);
            when(mockFile.getBytes()).thenReturn("dummy content".getBytes());
            when(mockFile.getContentType()).thenReturn("application/pdf");
            assertThatThrownBy(() -> minioStorageService.storeFile("failKey", mockFile))
                    .isInstanceOf(IOException.class);
        }
    }
}