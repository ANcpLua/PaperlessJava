package at.fhtw.services.integration;

import at.fhtw.services.OcrServiceImp;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import static at.fhtw.services.integration.IntegrationTestBase.OcrConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(IntegrationTestBase.SharedContainersExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("OcrServiceImp Integration Tests")
class OcrServiceImpIntegrationTest extends IntegrationTestBase {

    @Autowired
    private Tesseract tesseract;

    private OcrServiceImp ocrServiceImp;
    private File tempFile;

    @BeforeEach
    void setUp() {
        ocrServiceImp = new OcrServiceImp(tesseract, DPI);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempFile != null && tempFile.exists()) {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    @Test
    @DisplayName("OCR Service should successfully extract text from PDF containing actual text content")
    void testPdfTextExtraction() throws Exception {
        tempFile = createTestPdfWithText();
        String result = ocrServiceImp.extractText(tempFile);
        assertThat(result).containsIgnoringCase(HELLO_PDF_TEXT);
    }

    @Test
    @DisplayName("OCR Service should throw exception when processing PDF with invalid content")
    void testInvalidPdfContent() throws Exception {
        tempFile = File.createTempFile(INVALID_PDF_PREFIX, PDF_EXTENSION);
        Files.write(tempFile.toPath(), INVALID_CONTENT.getBytes());
        assertThatThrownBy(() -> ocrServiceImp.extractText(tempFile))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("OCR Service should process PDF files with uppercase extension (.PDF)")
    void testUppercasePdfExtension() throws Exception {
        tempFile = File.createTempFile(UPPERCASE_PDF_PREFIX, UPPERCASE_EXT);
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(tempFile);
        }
        String text = ocrServiceImp.extractText(tempFile);
        assertThat(text).isNotNull();
    }

    @Test
    @DisplayName("OCR Service should throw IndexOutOfBoundsException for empty PDF file")
    void testEmptyPdfProcessing() throws Exception {
        tempFile = File.createTempFile(EMPTY_PDF_PREFIX, PDF_EXTENSION);
        try (PDDocument document = new PDDocument()) {
            document.save(tempFile);
        }
        assertThatThrownBy(() -> ocrServiceImp.extractText(tempFile))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("OCR Service should handle null file reference")
    @SuppressWarnings("ConstantConditions")
    void testNullFileReference() {
        assertThatThrownBy(() -> ocrServiceImp.extractText(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("OCR Service should return minimal or empty text when processing a blank image")
    void testBlankImageExtraction() throws Exception {
        BufferedImage blankImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        g2d.dispose();
        tempFile = File.createTempFile("blank_image_", PNG_EXTENSION);
        ImageIO.write(blankImage, IMAGE_FORMAT, tempFile);
        String result = ocrServiceImp.extractText(tempFile);
        assertThat(result).isNotNull();
        assertThat(result.trim()).isEmpty();
    }

    private File createTestPdfWithText() throws Exception {
        File pdfFile = File.createTempFile(TEMP_PDF_PREFIX, PDF_EXTENSION);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, PDF_FONT_SIZE);
                contentStream.newLineAtOffset(PDF_TEXT_X, PDF_TEXT_Y);
                contentStream.showText(HELLO_PDF_TEXT);
                contentStream.endText();
            }
            document.save(pdfFile);
        }
        return pdfFile;
    }
}