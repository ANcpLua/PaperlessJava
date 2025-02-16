package at.fhtw.services.imp;

import java.io.File;

public interface IOcrService {
    String extractText(File file) throws Exception;
}