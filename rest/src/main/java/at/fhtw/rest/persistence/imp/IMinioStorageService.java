package at.fhtw.rest.persistence.imp;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface IMinioStorageService {
    void storeFile(String objectKey, MultipartFile file) throws IOException;

    Optional<byte[]> loadFile(String objectKey);

    void deleteFile(String objectKey);
}