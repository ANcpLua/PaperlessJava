package at.fhtw.services.unit;

import at.fhtw.services.OcrService;
import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileNotFoundException;

import static at.fhtw.services.unit.TestBase.OcrConstants.DELETE_FAILED_PREFIX;
import static at.fhtw.services.unit.TestBase.OcrConstants.MOCK_TEXT;
import static at.fhtw.services.unit.TestBase.OcrConstants.OCR_FAILED_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class OcrServiceTest {

    @Mock
    private Tesseract tesseract;

    private OcrService ocrService;
    private File tempFile;

    @BeforeEach
    void setUp() {
        ocrService = new OcrService(tesseract, 300);
    }

    @AfterEach
    void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                System.err.println(DELETE_FAILED_PREFIX + tempFile.getAbsolutePath());
            }
        }
    }

    @Nested
    class GivenTextFile {
        @Test
        void whenProcessingSucceeds_thenTextIsExtracted() throws Exception {
            tempFile = File.createTempFile("test-file-", ".txt");
            when(tesseract.doOCR(any(File.class))).thenReturn(MOCK_TEXT);

            String actualText = ocrService.extractText(tempFile);

            assertThat(actualText).isEqualTo(MOCK_TEXT);
            verify(tesseract).doOCR(tempFile);
        }

        @Test
        void whenOcrFails_thenExceptionIsPropagated() throws Exception {
            tempFile = File.createTempFile("test-file-", ".txt");
            when(tesseract.doOCR(tempFile)).thenThrow(new RuntimeException(OCR_FAILED_MESSAGE));

            assertThatThrownBy(() -> ocrService.extractText(tempFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(OCR_FAILED_MESSAGE);

            verify(tesseract).doOCR(tempFile);
        }
    }

    @Nested
    class GivenNonExistentFile {
        @Test
        void whenAttemptingOcr_thenShouldThrowFileNotFound() {
            File nonExistent = new File("does-not-exist");

            assertThatThrownBy(() -> ocrService.extractText(nonExistent))
                    .isInstanceOf(FileNotFoundException.class);
        }
    }
}