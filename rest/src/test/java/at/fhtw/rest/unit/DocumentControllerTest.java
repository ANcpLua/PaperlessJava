package at.fhtw.rest.unit;

import at.fhtw.rest.api.DocumentController;
import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.core.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class DocumentControllerTest {

    private static final String DOC_ID_VALID = "doc123";
    private static final String DOC_ID_BLANK = "";
    private static final String FILE_NAME_TEST_PDF = "test.pdf";
    private static final String FILE_NAME_NEW = "newname.pdf";
    private static final String FILE_NAME_WITH_EXTENSION = "name.pdf";
    private static final String PDF_CONTENT = "pdf content";
    private static final String SEARCH_QUERY = "test";
    private static final String DOC_ID_1 = "doc1";
    private static final String FILE_NAME_1 = "file1.pdf";
    private static final String DOC_ID_2 = "doc2";
    private static final String FILE_NAME_2 = "file2.pdf";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    @DisplayName("Successful upload returns 201 and DocumentRequest")
    void testUpload_Success() throws Exception {
        DocumentRequest responseDto = DocumentRequest.builder().id(DOC_ID_VALID).filename(FILE_NAME_TEST_PDF).build();
        MockMultipartFile file = new MockMultipartFile("file", FILE_NAME_TEST_PDF, "application/pdf", "dummy content".getBytes());
        when(documentService.uploadFile(any())).thenReturn(responseDto);
        mockMvc.perform(multipart("/documents").file(file)).andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(DOC_ID_VALID))).andExpect(jsonPath("$.filename", is(FILE_NAME_TEST_PDF)));
        verify(documentService, times(1)).uploadFile(any());
    }

    @Test
    @DisplayName("Upload without file returns 400")
    void testUpload_MissingFile() throws Exception {
        mockMvc.perform(multipart("/documents")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Successful rename returns 200 and updated DocumentRequest")
    void testRename_Success() throws Exception {
        DocumentRequest updatedDto = DocumentRequest.builder().id(DOC_ID_VALID).filename(FILE_NAME_NEW).build();
        when(documentService.renameFile(eq(DOC_ID_VALID), eq(FILE_NAME_NEW))).thenReturn(updatedDto);
        mockMvc.perform(patch("/documents/{id}", DOC_ID_VALID).param("newName", FILE_NAME_NEW)).andExpect(status().isOk()).andExpect(jsonPath("$.id", is(DOC_ID_VALID))).andExpect(jsonPath("$.filename", is(FILE_NAME_NEW)));
        verify(documentService, times(1)).renameFile(DOC_ID_VALID, FILE_NAME_NEW);
    }

    @Test
    @DisplayName("Rename with blank document ID returns 400")
    void testRename_BlankId() throws Exception {
        mockMvc.perform(patch("/documents/{id}", DOC_ID_BLANK).param("newName", FILE_NAME_WITH_EXTENSION)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Rename with blank newName returns 400")
    void testRename_BlankNewName() throws Exception {
        mockMvc.perform(patch("/documents/{id}", DOC_ID_VALID).param("newName", DOC_ID_BLANK)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Successful download returns PDF bytes")
    void testDownload_Success() throws Exception {
        byte[] fileBytes = PDF_CONTENT.getBytes();
        when(documentService.getFileBytes(eq(DOC_ID_VALID))).thenReturn(fileBytes);
        mockMvc.perform(get("/documents/{id}/download", DOC_ID_VALID)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andExpect(content().bytes(fileBytes));
        verify(documentService, times(1)).getFileBytes(DOC_ID_VALID);
    }

    @Test
    @DisplayName("Download with blank document ID returns 400")
    void testDownload_BlankId() throws Exception {
        mockMvc.perform(get("/documents/{id}/download", DOC_ID_BLANK)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Successful delete returns 204")
    void testDelete_Success() throws Exception {
        doNothing().when(documentService).deleteDocument(eq(DOC_ID_VALID));
        mockMvc.perform(delete("/documents/{id}", DOC_ID_VALID)).andExpect(status().isNoContent());
        verify(documentService, times(1)).deleteDocument(DOC_ID_VALID);
    }

    @Test
    @DisplayName("Delete with blank document ID returns 400")
    void testDelete_BlankId() throws Exception {
        mockMvc.perform(delete("/documents/{id}", DOC_ID_BLANK)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Successful get document returns 200 and DocumentRequest")
    void testGetDocument_Success() throws Exception {
        DocumentRequest dto = DocumentRequest.builder().id(DOC_ID_VALID).filename(FILE_NAME_TEST_PDF).build();
        when(documentService.getDocument(eq(DOC_ID_VALID))).thenReturn(dto);
        mockMvc.perform(get("/documents/{id}", DOC_ID_VALID)).andExpect(status().isOk()).andExpect(jsonPath("$.id", is(DOC_ID_VALID))).andExpect(jsonPath("$.filename", is(FILE_NAME_TEST_PDF)));
        verify(documentService, times(1)).getDocument(DOC_ID_VALID);
    }

    @Test
    @DisplayName("Get document with blank document ID returns 400")
    void testGetDocument_BlankId() throws Exception {
        mockMvc.perform(get("/documents/{id}", DOC_ID_BLANK)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Successful search returns 200 and a list of DocumentRequests")
    void testSearch_Success() throws Exception {
        DocumentRequest dto1 = DocumentRequest.builder().id(DOC_ID_1).filename(FILE_NAME_1).build();
        DocumentRequest dto2 = DocumentRequest.builder().id(DOC_ID_2).filename(FILE_NAME_2).build();
        List<DocumentRequest> results = Arrays.asList(dto1, dto2);
        when(documentService.searchDocuments(eq(SEARCH_QUERY))).thenReturn(results);
        mockMvc.perform(get("/documents/search").param("query", SEARCH_QUERY)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].id", is(DOC_ID_1))).andExpect(jsonPath("$[0].filename", is(FILE_NAME_1))).andExpect(jsonPath("$[1].id", is(DOC_ID_2))).andExpect(jsonPath("$[1].filename", is(FILE_NAME_2)));
        verify(documentService, times(1)).searchDocuments(SEARCH_QUERY);
    }

    @Test
    @DisplayName("Search with whitespace-only query returns empty result list")
    void testSearchWithWhitespaceQuery() throws Exception {
        mockMvc.perform(get("/documents/search")
                        .param("query", "   "))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Search with empty string query returns empty result list")
    void testSearchWithEmptyQuery() throws Exception {
        mockMvc.perform(get("/documents/search")
                        .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Search with null query returns empty result list")
    void testSearchWithNullQuery() throws Exception {
        mockMvc.perform(get("/documents/search"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}