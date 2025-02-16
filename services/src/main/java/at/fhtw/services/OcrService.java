package at.fhtw.services;

import at.fhtw.services.imp.IOcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;

@Slf4j
@Service
public class OcrService implements IOcrService {
    private final Tesseract tesseract;
    private final int dpi;

    public OcrService(Tesseract tesseract, @Value("${tesseract.dpi:300}") int dpi) {
        this.tesseract = tesseract;
        this.dpi = dpi;
    }

    @Override
    public String extractText(File file) throws Exception {
        if (file == null) {
            log.error("OCR extraction failed: Provided file reference is null.");
            throw new NullPointerException("File is null");
        }
        if (!file.exists()) {
            log.error("OCR extraction failed: File not found. Provided file path: {}", file.getAbsolutePath());
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        log.info("Starting OCR extraction for file: {}", file.getAbsolutePath());
        String fileName = file.getName().toLowerCase();
        try {
            String extractedText;
            if (fileName.endsWith(".pdf")) {
                try (PDDocument doc = PDDocument.load(file)) {
                    if (doc.getNumberOfPages() == 0) {
                        log.error("OCR extraction failed: The PDF file {} has no pages.", file.getAbsolutePath());
                        throw new IndexOutOfBoundsException("Empty PDF file");
                    }
                    PDFRenderer renderer = new PDFRenderer(doc);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < doc.getNumberOfPages(); i++) {
                        BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                        sb.append(tesseract.doOCR(image));
                    }
                    extractedText = sb.toString();
                }
            } else {
                extractedText = tesseract.doOCR(file);
            }
            log.info("Completed OCR extraction for file: {}. Extracted text length: {}",
                    file.getAbsolutePath(), extractedText.length());
            return extractedText;
        } catch (Exception e) {
            log.error("OCR extraction failed for file: {}. Error: {}",
                    file.getAbsolutePath(), e.getMessage(), e);
            throw e;
        }
    }
}