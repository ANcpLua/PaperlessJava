package at.fhtw.rest.unit;

import at.fhtw.rest.core.ElasticsearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ElasticsearchServiceTest {

    private static final String DOC_ID = "test-doc-123";
    private static final String NEW_FILENAME = "updated-file.pdf";
    private static final String UPDATE_FAILED_MSG = "Update failed";
    private static final String DELETE_FAILED_MSG = "Delete failed";
    private static final String QUERY_TEST = "test query";
    private static final String DOC_ID_1 = "doc1";
    private static final String DOC_ID_2 = "doc2";

    @Mock
    private ElasticsearchClient esClient;

    private ElasticsearchService elasticsearchService;

    @BeforeEach
    void setUp() {
        elasticsearchService = new ElasticsearchService(esClient, "documents");
    }

    @Nested
    @DisplayName("Update Filename Tests")
    class UpdateFilenameTests {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Should successfully update filename")
        void shouldSuccessfullyUpdateFilename() throws IOException {
            var mockResponse = mock(UpdateResponse.class);
            doReturn(mockResponse)
                    .when(esClient)
                    .update(any(UpdateRequest.class), eq(Map.class));
            elasticsearchService.updateFilename(DOC_ID, NEW_FILENAME);
            verify(esClient).update(any(UpdateRequest.class), eq(Map.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Should handle IOException during filename update")
        void shouldHandleIOExceptionDuringUpdate() throws IOException {
            doThrow(new IOException(UPDATE_FAILED_MSG))
                    .when(esClient)
                    .update(any(UpdateRequest.class), eq(Map.class));
            elasticsearchService.updateFilename(DOC_ID, NEW_FILENAME);
            verify(esClient).update(any(UpdateRequest.class), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("Delete Document Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should successfully delete document")
        void shouldSuccessfullyDeleteDocument() throws IOException {
            DeleteResponse mockResponse = mock(DeleteResponse.class);
            doReturn(mockResponse)
                    .when(esClient)
                    .delete(any(DeleteRequest.class));
            elasticsearchService.deleteDocument(DOC_ID);
            verify(esClient).delete(any(DeleteRequest.class));
        }

        @Test
        @DisplayName("Should handle IOException during document deletion")
        void shouldHandleIOExceptionDuringDeletion() throws IOException {
            doThrow(new IOException(DELETE_FAILED_MSG))
                    .when(esClient)
                    .delete(any(DeleteRequest.class));
            elasticsearchService.deleteDocument(DOC_ID);
            verify(esClient).delete(any(DeleteRequest.class));
        }
    }

    @Nested
    @DisplayName("Search IDs By Query Tests")
    class SearchIdsByQueryTests {

        @Test
        @DisplayName("Should return empty list for null query")
        void shouldReturnEmptyListForNullQuery() {
            List<String> results = elasticsearchService.searchIdsByQuery(null);
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for blank query")
        void shouldReturnEmptyListForBlankQuery() {
            List<String> results = elasticsearchService.searchIdsByQuery("   ");
            assertThat(results).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Should successfully search and return document IDs")
        void shouldSuccessfullySearchAndReturnIds() throws IOException {
            var mockResponse = mock(SearchResponse.class);
            var mockHits = mock(HitsMetadata.class);
            List<Hit<Map<String, Object>>> hitsList = new ArrayList<>();
            Hit<Map<String, Object>> hit1 = mock((Class<Hit<Map<String, Object>>>) (Class<?>) Hit.class);
            when(hit1.id()).thenReturn(DOC_ID_1);
            Hit<Map<String, Object>> hit2 = mock((Class<Hit<Map<String, Object>>>) (Class<?>) Hit.class);
            when(hit2.id()).thenReturn(DOC_ID_2);
            assertTrue(hitsList.add(hit1));
            assertTrue(hitsList.add(hit2));
            when(mockResponse.hits()).thenReturn(mockHits);
            when(mockHits.hits()).thenReturn(hitsList);
            doReturn(mockResponse)
                    .when(esClient)
                    .search(any(Function.class), eq(Map.class));
            List<String> results = elasticsearchService.searchIdsByQuery(QUERY_TEST);
            assertThat(results).hasSize(2).containsExactly(DOC_ID_1, DOC_ID_2);
            verify(esClient).search(any(Function.class), eq(Map.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Should handle IOException during search")
        void shouldHandleIOExceptionDuringSearch() throws IOException {
            doThrow(new IOException("Search failed"))
                    .when(esClient)
                    .search(any(Function.class), eq(Map.class));
            List<String> results = elasticsearchService.searchIdsByQuery(QUERY_TEST);
            assertThat(results).isEmpty();
            verify(esClient).search(any(Function.class), eq(Map.class));
        }
    }
}