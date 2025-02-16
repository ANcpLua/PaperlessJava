package at.fhtw.rest.unit;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.infrastructure.mapper.DocumentMapper;
import at.fhtw.rest.persistence.DocumentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class DocumentMapperTest {

    private DocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        documentMapper = new DocumentMapper();
    }

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("toEntity - should correctly map a valid DocumentRequest")
        void testToEntityWithValidRequest() {
            DocumentRequest request = DocumentRequest.builder()
                    .id("123")
                    .filename("test.pdf")
                    .filesize(1024L)
                    .filetype("application/pdf")
                    .uploadDate(LocalDateTime.of(2025, 2, 9, 12, 0))
                    .ocrJobDone(false)
                    .ocrText("Sample OCR text")
                    .build();

            DocumentEntity entity = documentMapper.toEntity(request);

            assertThat(entity).as("Mapped entity should not be null").isNotNull();
            assertThat(entity.getId()).as("ID should be mapped correctly").isEqualTo("123");
            assertThat(entity.getFilename()).as("Filename should be mapped correctly").isEqualTo("test.pdf");
            assertThat(entity.getFilesize()).as("Filesize should be mapped correctly").isEqualTo(1024L);
            assertThat(entity.getFiletype()).as("Filetype should be mapped correctly").isEqualTo("application/pdf");
            assertThat(entity.getUploadDate()).as("Upload date should be mapped correctly")
                    .isEqualTo(LocalDateTime.of(2025, 2, 9, 12, 0));
            assertThat(entity.isOcrJobDone()).as("OCR job done flag should be mapped correctly").isEqualTo(false);
            assertThat(entity.getOcrText()).as("OCR text should be mapped correctly").isEqualTo("Sample OCR text");
            assertThat(entity.getObjectKey()).as("Object key should be set to the document id").isEqualTo("123");
        }

        @Test
        @DisplayName("toEntity - should return null when given a null DocumentRequest")
        void testToEntityWithNullRequest() {
            DocumentEntity entity = documentMapper.toEntity(null);
            assertThat(entity).as("Mapping null DocumentRequest should return null").isNull();
        }
    }

    @Nested
    @DisplayName("toDto Tests")
    class ToDtoTests {

        @Test
        @DisplayName("toDto - should correctly map a valid DocumentEntity")
        void testToDtoWithValidEntity() {
            DocumentEntity entity = new DocumentEntity();
            entity.setId("456");
            entity.setFilename("document.pdf");
            entity.setFilesize(2048L);
            entity.setFiletype("application/pdf");
            entity.setUploadDate(LocalDateTime.of(2025, 2, 9, 12, 30));
            entity.setOcrJobDone(true);
            entity.setOcrText("Extracted text");

            DocumentRequest dto = documentMapper.toDto(entity);

            assertThat(dto).as("Mapped DTO should not be null").isNotNull();
            assertThat(dto.getId()).as("ID should be mapped correctly").isEqualTo("456");
            assertThat(dto.getFilename()).as("Filename should be mapped correctly").isEqualTo("document.pdf");
            assertThat(dto.getFilesize()).as("Filesize should be mapped correctly").isEqualTo(2048L);
            assertThat(dto.getFiletype()).as("Filetype should be mapped correctly").isEqualTo("application/pdf");
            assertThat(dto.getUploadDate()).as("Upload date should be mapped correctly")
                    .isEqualTo(LocalDateTime.of(2025, 2, 9, 12, 30));
            assertThat(dto.isOcrJobDone()).as("OCR job done flag should be mapped correctly").isEqualTo(true);
            assertThat(dto.getOcrText()).as("OCR text should be mapped correctly").isEqualTo("Extracted text");
        }

        @Test
        @DisplayName("toDto - should return null when given a null DocumentEntity")
        void testToDtoWithNullEntity() {
            DocumentRequest dto = documentMapper.toDto(null);
            assertThat(dto).as("Mapping null DocumentEntity should return null").isNull();
        }
    }
}