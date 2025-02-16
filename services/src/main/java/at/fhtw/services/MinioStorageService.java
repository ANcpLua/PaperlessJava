package at.fhtw.services;

import at.fhtw.services.imp.IMinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class MinioStorageService implements IMinioStorageService {
    private final MinioClient minioClient;

    @Override
    public File downloadFile(String documentId, String extension) throws Exception {
        log.info("[REQUEST] Entering downloadFile with documentId: {} and extension: {}", documentId, extension);
        File tempFile = new File(System.getProperty("java.io.tmpdir"), documentId + extension);
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket("documents").object(documentId).build());
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            inputStream.transferTo(fos);
            log.info("[RESPONSE] Exiting downloadFile for documentId: {}; file: {}",
                    documentId, tempFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("[ERROR] downloadFile failed for documentId: {}. Error: {}",
                    documentId, e.getMessage(), e);
            throw e;
        }
        return tempFile;
    }
}