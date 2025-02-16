package at.fhtw.services;

import java.io.File;

public interface MinioStorageService {
    File downloadFile(String documentId, String extension) throws Exception;
}