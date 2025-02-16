package at.fhtw.rest.persistence;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Service
public class MinioStorageServiceImp implements MinioStorageService {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageServiceImp(MinioClient minioClient, @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public void storeFile(String objectKey, MultipartFile file) throws IOException {
        try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created.", bucketName);
            }
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IOException("Failed to store file", e);
        }
    }

    @Override
    public Optional<byte[]> loadFile(String objectKey) {
        try (var response = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectKey).build())) {
            byte[] bytes = response.readAllBytes();
            return Optional.of(bytes);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                log.warn("Bucket '{}' does not exist", bucketName);
                return;
            }
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectKey).build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}