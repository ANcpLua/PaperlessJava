package at.fhtw.rest.integration;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.core.DocumentService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Key Resources:
 * <ul>
 *   <li><a href="https://www.testcontainers.org/modules/databases/postgres/">Testcontainers PostgreSQL Guide</a></li>
 *   <li><a href="https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing">Spring Boot Testing</a></li>
 *   <li><a href="https://rest-assured.io/">REST Assured</a></li>
 *   <li><a href="https://site.mockito.org/">Mockito</a></li>
 * </ul>
 *
 * For Container Config:
 * <ul>
 *   <li><a href="https://www.testcontainers.org/features/networking/">Container Networking</a></li>
 *   <li><a href="https://www.testcontainers.org/features/files/">File Mapping</a></li>
 * </ul>
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentControllerIntegrationTest {

    @LocalServerPort
    private Integer port;

    @MockBean
    private DocumentService documentService;

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test
    void shouldUploadDocument() throws Exception {
        String docId = UUID.randomUUID().toString();
        DocumentRequest expectedResponse = createDocumentRequest(docId, "test.pdf");
        when(documentService.uploadFile(any())).thenReturn(expectedResponse);
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test content".getBytes());

        given()
                .multiPart("file", file.getOriginalFilename(), file.getBytes(), file.getContentType())
                .when()
                .post("/documents")
                .then()
                .statusCode(201)
                .body("id", equalTo(docId))
                .body("filename", equalTo("test.pdf"));
    }

    @Test
    void shouldGetDocument() {
        String docId = UUID.randomUUID().toString();
        DocumentRequest expectedDoc = createDocumentRequest(docId, "test.pdf");
        when(documentService.getDocument(docId)).thenReturn(expectedDoc);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/documents/" + docId)
                .then()
                .statusCode(200)
                .body("id", equalTo(docId))
                .body("filename", equalTo("test.pdf"));
    }

    @Test
    void shouldDownloadDocument() {
        String docId = UUID.randomUUID().toString();
        byte[] fileContent = "test content".getBytes();
        when(documentService.getFileBytes(docId)).thenReturn(fileContent);

        given()
                .when()
                .get("/documents/" + docId + "/download")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .body(equalTo(new String(fileContent)));
    }

    @Test
    void shouldRenameDocument() throws Exception {
        String docId = UUID.randomUUID().toString();
        String newName = "renamed.pdf";
        DocumentRequest expectedDoc = createDocumentRequest(docId, newName);
        when(documentService.renameFile(eq(docId), eq(newName))).thenReturn(expectedDoc);

        given()
                .queryParam("newName", newName)
                .when()
                .patch("/documents/" + docId)
                .then()
                .statusCode(200)
                .body("id", equalTo(docId))
                .body("filename", equalTo(newName));
    }

    @Test
    void shouldFailRenameWhenServiceThrowsException() throws IOException {
        String docId = UUID.randomUUID().toString();
        when(documentService.renameFile(eq(docId), any())).thenThrow(new RuntimeException("Service error"));

        given()
                .queryParam("newName", "newfile.pdf")
                .when()
                .patch("/documents/" + docId)
                .then()
                .statusCode(500);
    }

    @Test
    void shouldDeleteDocument() {
        String docId = UUID.randomUUID().toString();
        doNothing().when(documentService).deleteDocument(docId);

        given()
                .when()
                .delete("/documents/" + docId)
                .then()
                .statusCode(204);

        verify(documentService).deleteDocument(docId);
    }

    @Test
    void shouldSearchDocuments() {
        List<DocumentRequest> expectedDocs = List.of(
                createDocumentRequest(UUID.randomUUID().toString(), "test1.pdf"),
                createDocumentRequest(UUID.randomUUID().toString(), "test2.pdf")
        );
        when(documentService.searchDocuments("test")).thenReturn(expectedDocs);

        given()
                .queryParam("query", "test")
                .when()
                .get("/documents/search")
                .then()
                .statusCode(200)
                .body(".", hasSize(2))
                .body("[0].filename", equalTo("test1.pdf"))
                .body("[1].filename", equalTo("test2.pdf"));
    }

    @Test
    void shouldHandleServiceException() {
        String docId = UUID.randomUUID().toString();
        when(documentService.getDocument(docId)).thenThrow(new RuntimeException("Document not found"));

        given()
                .when()
                .get("/documents/" + docId)
                .then()
                .statusCode(500);
    }

    @Test
    void shouldFailRenameWhenNewNameIsBlank() {
        String docId = UUID.randomUUID().toString();

        given()
                .queryParam("newName", "")
                .when()
                .patch("/documents/" + docId)
                .then()
                .statusCode(400)
                .body("error", containsString("New name must not be blank"));
    }

    @Test
    void shouldFailUploadWhenFileIsNull() {
        given()
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .when()
                .post("/documents")
                .then()
                .statusCode(400)
                .body("error", containsString("File must not be null"));
    }

    @Test
    void shouldFailRenameWhenDocIdIsBlank() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("newName", "test.pdf")
                .when()
                .patch("/documents/" + " ")
                .then()
                .statusCode(400)
                .body("error", containsString("Document ID must not be blank"));
    }

    @Test
    void shouldReturnEmptyListWhenSearchQueryIsBlank() {
        given()
                .queryParam("query", "")
                .when()
                .get("/documents/search")
                .then()
                .statusCode(200)
                .body(".", hasSize(0));
    }

    @Test
    void shouldHandleIOException() throws IOException {
        String docId = UUID.randomUUID().toString();
        when(documentService.renameFile(eq(docId), any())).thenThrow(new IOException("Storage error"));

        given()
                .queryParam("newName", "test.pdf")
                .when()
                .patch("/documents/" + docId)
                .then()
                .statusCode(500)
                .body("error", equalTo("Internal server error"));
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        String docId = UUID.randomUUID().toString();
        when(documentService.getDocument(docId)).thenThrow(new IllegalArgumentException("Document not found"));

        given()
                .when()
                .get("/documents/" + docId)
                .then()
                .statusCode(400)
                .body("error", equalTo("Document not found"));
    }

    @Test
    void shouldGetAllDocuments() {
        List<DocumentRequest> expectedDocs = List.of(
                createDocumentRequest("id1", "filename1.pdf"),
                createDocumentRequest("id2", "filename2.pdf")
        );
        when(documentService.getAllDocuments()).thenReturn(expectedDocs);

        given()
                .when()
                .get("/documents")
                .then()
                .statusCode(200)
                .body(".", hasSize(2))
                .body("[0].id", equalTo("id1"))
                .body("[0].filename", equalTo("filename1.pdf"))
                .body("[1].id", equalTo("id2"))
                .body("[1].filename", equalTo("filename2.pdf"));
    }

    private DocumentRequest createDocumentRequest(String id, String filename) {
        return DocumentRequest.builder()
                .id(id)
                .filename(filename)
                .filesize(0L)
                .filetype(MediaType.APPLICATION_PDF_VALUE)
                .uploadDate(LocalDateTime.now())
                .ocrJobDone(false)
                .ocrText("")
                .build();
    }
}