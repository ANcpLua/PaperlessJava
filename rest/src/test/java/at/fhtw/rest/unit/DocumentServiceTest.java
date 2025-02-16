package at.fhtw.rest.unit;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.core.DocumentService;
import at.fhtw.rest.core.imp.IElasticsearchService;
import at.fhtw.rest.infrastructure.mapper.imp.IDocumentMapper;
import at.fhtw.rest.message.imp.IProcessingEventDispatcher;
import at.fhtw.rest.persistence.DocumentEntity;
import at.fhtw.rest.persistence.imp.IDocumentRepository;
import at.fhtw.rest.persistence.imp.IMinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class DocumentServiceTest {

    private static final String TEST_FILENAME = "testfile.pdf";
    private static final String NEW_FILENAME = "newfile.pdf";
    private static final String TEST_DOC_ID = "test-doc-id";
    private static final byte[] TEST_FILE_BYTES = "dummy content".getBytes();
    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String ERROR_FILE_EMPTY = "File must not be empty";
    private static final String ERROR_DOCUMENT_NOT_FOUND = "Document not found";
    private static final String ERROR_FILE_NOT_FOUND = "File not found";
    private static final String SEARCH_QUERY_VALID = "search term";
    private static final String SEARCH_QUERY_NON_EXISTENT = "non-existent";

    @Mock
    private IDocumentRepository documentRepository;
    @Mock
    private IDocumentMapper mapper;
    @Mock
    private IMinioStorageService minioStorageService;
    @Mock
    private IProcessingEventDispatcher processingEventDispatcher;
    @Mock
    private IElasticsearchService elasticsearchService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                mapper,
                minioStorageService,
                processingEventDispatcher,
                elasticsearchService
        );
    }

    private MultipartFile createMockMultipartFile(byte[] content) {
        return new MockMultipartFile("file", TEST_FILENAME, MIME_TYPE_PDF, content);
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("uploadFile - successful upload")
        void uploadFileSuccessful() throws IOException {
            MultipartFile file = createMockMultipartFile(TEST_FILE_BYTES);
            doAnswer(invocation -> {
                DocumentEntity entity = invocation.getArgument(0);
                entity.setId(TEST_DOC_ID);
                return null;
            }).when(documentRepository).save(any(DocumentEntity.class));
            DocumentRequest expectedDto = DocumentRequest.builder().build();
            when(mapper.toDto(any(DocumentEntity.class))).thenReturn(expectedDto);

            DocumentRequest result = documentService.uploadFile(file);
            assertThat(result).isNotNull();

            InOrder inOrder = inOrder(minioStorageService, documentRepository, mapper, processingEventDispatcher);
            inOrder.verify(minioStorageService).storeFile(anyString(), eq(file));
            inOrder.verify(documentRepository).save(any(DocumentEntity.class));
            inOrder.verify(mapper).toDto(any(DocumentEntity.class));
            inOrder.verify(processingEventDispatcher).sendProcessingRequest(anyString(), eq(TEST_FILENAME));
        }

        @Test
        @DisplayName("renameFile - successful rename")
        void renameFileSuccessful() {
            DocumentEntity entity = new DocumentEntity();
            entity.setId(TEST_DOC_ID);
            entity.setFilename(TEST_FILENAME);
            when(documentRepository.findById(TEST_DOC_ID)).thenReturn(Optional.of(entity));
            DocumentRequest expectedDto = DocumentRequest.builder().build();
            when(mapper.toDto(entity)).thenReturn(expectedDto);

            DocumentRequest result = documentService.renameFile(TEST_DOC_ID, NEW_FILENAME);
            assertThat(result).isNotNull();
            assertThat(entity.getFilename()).isEqualTo(NEW_FILENAME);

            InOrder inOrder = inOrder(minioStorageService, elasticsearchService, documentRepository, mapper);
            inOrder.verify(elasticsearchService).updateFilename(TEST_DOC_ID, NEW_FILENAME);
            inOrder.verify(documentRepository).findById(TEST_DOC_ID);
            inOrder.verify(documentRepository).save(entity);
            inOrder.verify(mapper).toDto(entity);
        }

        @Test
        @DisplayName("getFileBytes - file found")
        void getFileBytesSuccessful() {
            when(minioStorageService.loadFile(TEST_DOC_ID)).thenReturn(Optional.of(TEST_FILE_BYTES));
            byte[] result = documentService.getFileBytes(TEST_DOC_ID);
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(TEST_FILE_BYTES);
        }

        @Test
        @DisplayName("deleteDocument - successful deletion")
        void deleteDocumentSuccessful() {
            documentService.deleteDocument(TEST_DOC_ID);
            InOrder inOrder = inOrder(minioStorageService, documentRepository, elasticsearchService);
            inOrder.verify(minioStorageService).deleteFile(TEST_DOC_ID);
            inOrder.verify(documentRepository).deleteById(TEST_DOC_ID);
            inOrder.verify(elasticsearchService).deleteDocument(TEST_DOC_ID);
        }

        @Test
        @DisplayName("getDocument - document found")
        void getDocumentSuccessful() {
            DocumentEntity entity = new DocumentEntity();
            entity.setId(TEST_DOC_ID);
            entity.setFilename(TEST_FILENAME);
            when(documentRepository.findById(TEST_DOC_ID)).thenReturn(Optional.of(entity));
            DocumentRequest expectedDto = DocumentRequest.builder().build();
            when(mapper.toDto(entity)).thenReturn(expectedDto);

            DocumentRequest result = documentService.getDocument(TEST_DOC_ID);
            assertThat(result).isNotNull();
            verify(documentRepository).findById(TEST_DOC_ID);
            verify(mapper).toDto(entity);
        }

        @Test
        @DisplayName("searchDocuments - valid query returns results")
        void searchDocumentsSuccessful() {
            when(elasticsearchService.searchIdsByQuery(SEARCH_QUERY_VALID)).thenReturn(Collections.singletonList(TEST_DOC_ID));
            DocumentEntity entity = new DocumentEntity();
            entity.setId(TEST_DOC_ID);
            entity.setFilename(TEST_FILENAME);
            when(documentRepository.findById(TEST_DOC_ID)).thenReturn(Optional.of(entity));
            DocumentRequest expectedDto = DocumentRequest.builder().build();
            when(mapper.toDto(entity)).thenReturn(expectedDto);

            List<DocumentRequest> results = documentService.searchDocuments(SEARCH_QUERY_VALID);
            assertThat(results).isNotNull();
            assertThat(results).isNotEmpty().hasSize(1);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("uploadFile - empty file should throw exception")
        void uploadFileEmptyFileThrows() {
            MultipartFile file = createMockMultipartFile(new byte[0]);
            assertThatThrownBy(() -> documentService.uploadFile(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ERROR_FILE_EMPTY);
        }

        @Test
        @DisplayName("renameFile - document not found should throw exception")
        void renameFileDocumentNotFoundThrows() {
            when(documentRepository.findById(TEST_DOC_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> documentService.renameFile(TEST_DOC_ID, NEW_FILENAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ERROR_DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("getFileBytes - file not found should throw exception")
        void getFileBytesFileNotFoundThrows() {
            when(minioStorageService.loadFile(TEST_DOC_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> documentService.getFileBytes(TEST_DOC_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ERROR_FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("getDocument - document not found should throw exception")
        void getDocumentNotFoundThrows() {
            when(documentRepository.findById(TEST_DOC_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> documentService.getDocument(TEST_DOC_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ERROR_DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("searchDocuments - null result from Elasticsearch returns empty list")
        void searchDocumentsNullResultReturnsEmptyList() {
            when(elasticsearchService.searchIdsByQuery(SEARCH_QUERY_NON_EXISTENT)).thenReturn(null);
            List<DocumentRequest> results = documentService.searchDocuments(SEARCH_QUERY_NON_EXISTENT);
            assertThat(results).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllDocuments Tests")
    class GetAllDocumentsTests {

        @Test
        @DisplayName("getAllDocuments returns list of DocumentRequest")
        void returnsListOfDocuments() {
            DocumentEntity e1 = new DocumentEntity();
            e1.setId("doc1");
            DocumentEntity e2 = new DocumentEntity();
            e2.setId("doc2");
            when(documentRepository.findAll()).thenReturn(List.of(e1, e2));
            DocumentRequest dto1 = DocumentRequest.builder().id("doc1").build();
            DocumentRequest dto2 = DocumentRequest.builder().id("doc2").build();
            when(mapper.toDto(e1)).thenReturn(dto1);
            when(mapper.toDto(e2)).thenReturn(dto2);

            List<DocumentRequest> result = documentService.getAllDocuments();
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo(dto1);
            assertThat(result.get(1)).isEqualTo(dto2);
        }

        @Test
        @DisplayName("getAllDocuments returns empty list when no documents")
        void returnsEmptyList() {
            when(documentRepository.findAll()).thenReturn(Collections.emptyList());
            List<DocumentRequest> result = documentService.getAllDocuments();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("ensurePdfExtension Tests")
    class EnsurePdfExtensionTests {

        @ParameterizedTest
        @CsvSource({
                "myfile, myfile.pdf",
                "myfile.pdf, myfile.pdf",
                "myfile.pdf.pdf, myfile.pdf",
                "myfile.pdf.pdf.pdf, myfile.pdf",
                "myfile.doc, myfile.pdf",
                "myfile., myfile.pdf"
        })
        @DisplayName("ensurePdfExtension properly fixes filenames")
        void ensuresExtension(String input, String expected) {
            String result = documentService.ensurePdfExtension(input);
            assertThat(result).isEqualTo(expected);
        }
    }
}