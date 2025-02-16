package at.fhtw.services;

import java.io.File;

public interface OcrService {
    String extractText(File file) throws Exception;
}