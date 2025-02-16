package at.fhtw.services.imp;

import java.io.File;

public interface IMinioStorageService {
    File downloadFile(String documentId, String extension) throws Exception;
}