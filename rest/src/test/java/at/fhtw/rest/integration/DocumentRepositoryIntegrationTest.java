package at.fhtw.rest.integration;

import at.fhtw.rest.persistence.DocumentEntity;
import at.fhtw.rest.persistence.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DocumentRepositoryIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        Startables.deepStart(Stream.of(POSTGRES)).join();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                String.format("jdbc:postgresql://localhost:%d/%s",
                        POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void testSaveAndFindById() {
        DocumentEntity doc = new DocumentEntity();
        doc.setId("doc-001");
        doc.setFilename("test_file.pdf");
        doc.setFilesize(12345L);
        doc.setFiletype("application/pdf");
        doc.setObjectKey("some/s3/path/test_file.pdf");
        doc.setUploadDate(LocalDateTime.now());
        doc.setOcrJobDone(false);
        doc.setOcrText(null);

        documentRepository.save(doc);

        Optional<DocumentEntity> found = documentRepository.findById("doc-001");
        assertThat(found)
                .as("Expected document to be found by ID 'doc-001'")
                .isPresent();
        assertThat(found.get().getFilename()).isEqualTo("test_file.pdf");
    }

    @Test
    void testUpdateDocument() {
        DocumentEntity doc = new DocumentEntity();
        doc.setId("doc-002");
        doc.setFilename("old_name.pdf");
        doc.setFilesize(500L);
        doc.setFiletype("application/pdf");
        doc.setObjectKey("some/s3/path/old_name.pdf");
        doc.setUploadDate(LocalDateTime.now());
        doc.setOcrJobDone(false);
        doc.setOcrText(null);

        documentRepository.save(doc);

        DocumentEntity savedDoc = documentRepository.findById("doc-002").orElseThrow();
        savedDoc.setFilename("new_name.pdf");
        savedDoc.setFilesize(600L);

        documentRepository.save(savedDoc);

        DocumentEntity updatedDoc = documentRepository.findById("doc-002").orElseThrow();
        assertThat(updatedDoc.getFilename()).isEqualTo("new_name.pdf");
        assertThat(updatedDoc.getFilesize()).isEqualTo(600L);
    }

    @Test
    void testDeleteDocument() {
        DocumentEntity doc = new DocumentEntity();
        doc.setId("doc-003");
        doc.setFilename("to_be_deleted.pdf");
        doc.setFilesize(200L);
        doc.setFiletype("application/pdf");
        doc.setUploadDate(LocalDateTime.now());

        documentRepository.save(doc);

        Optional<DocumentEntity> savedDoc = documentRepository.findById("doc-003");
        assertThat(savedDoc).isPresent();

        documentRepository.deleteById("doc-003");

        Optional<DocumentEntity> deletedDoc = documentRepository.findById("doc-003");
        assertThat(deletedDoc).isEmpty();
    }
}