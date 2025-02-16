package at.fhtw.services.unit;

import at.fhtw.services.configuration.ProcessorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static at.fhtw.services.unit.TestBase.ProcessorConfigTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ProcessorConfigTest {
    @Test
    @DisplayName("isValidTessdataPath returns false for null or blank input")
    void testIsValidTessdataPath_nullOrBlank() {
        ProcessorConfig config = new ProcessorConfig();
        assertThat(config.isValidTessdataPath(null)).isFalse();
        assertThat(config.isValidTessdataPath("    ")).isFalse();
    }

    @Test
    @DisplayName("isValidTessdataPath returns true for a directory that contains eng.traineddata")
    void testIsValidTessdataPath_validDirectory(@TempDir Path tempDir) throws IOException {
        File tessdataDir = tempDir.toFile();
        File trainedData = new File(tessdataDir, TRAINED_DATA);
        assertThat(trainedData.createNewFile()).isTrue();
        ProcessorConfig config = new ProcessorConfig();
        assertThat(config.isValidTessdataPath(tessdataDir.getAbsolutePath())).isTrue();
    }

    @Test
    @DisplayName("isValidTessdataPath returns false for a directory missing eng.traineddata")
    void testIsValidTessdataPath_missingTrainedData(@TempDir Path tempDir) {
        File tessdataDir = tempDir.toFile();
        ProcessorConfig config = new ProcessorConfig();
        assertThat(config.isValidTessdataPath(tessdataDir.getAbsolutePath())).isFalse();
    }

    @Test
    @DisplayName("findTessdataPath returns the configured path if it is valid")
    void testFindTessdataPath_returnsConfiguredPathIfValid(@TempDir Path tempDir) throws IOException {
        File validDir = tempDir.toFile();
        File trainedData = new File(validDir, TRAINED_DATA);
        assertThat(trainedData.createNewFile()).isTrue();
        ProcessorConfig config = new ProcessorConfig();
        String result = invokeMethod(config, "findTessdataPath", validDir.getAbsolutePath());
        assertThat(result).isEqualTo(validDir.getAbsolutePath());
    }

    @Test
    @DisplayName("findTessdataPath returns a fallback when the configured path is invalid")
    void testFindTessdataPath_returnsFallbackWhenConfiguredPathInvalid(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            File tessdataDir = tempDir.resolve("tessdata").toFile();
            assertThat(tessdataDir.mkdirs()).isTrue();
            File trainedData = new File(tessdataDir, TRAINED_DATA);
            assertThat(trainedData.createNewFile()).isTrue();
            ProcessorConfig config = new ProcessorConfig();
            String result = invokeMethod(config, "findTessdataPath", INVALID_TESSDATA_PATH);
            assertThat(result).isEqualTo(tessdataDir.getAbsolutePath());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("findTessdataPath throws IllegalStateException when no valid tessdata path is found")
    void testFindTessdataPath_throwsExceptionWhenNoValidPathFound() {
        ProcessorConfig config = new ProcessorConfig() {
            @Override
            public boolean isValidTessdataPath(String path) {
                return false;
            }
        };
        assertThatThrownBy(() -> invokeMethod(config, "findTessdataPath", INVALID_TESSDATA_PATH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ERROR_MESSAGE);
    }
}