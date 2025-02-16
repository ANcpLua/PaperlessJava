package at.fhtw.services.unit;

import at.fhtw.services.ElasticsearchIndexService;
import at.fhtw.services.imp.IElasticsearchIndexService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static at.fhtw.services.unit.TestBase.ElasticsearchConstants.*;
import static at.fhtw.services.unit.TestBase.MinioConstants.DOCUMENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ElasticsearchIndexServiceTest {

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private IndexResponse indexResponse;

    @Captor
    private ArgumentCaptor<IndexRequest<Map<String, Object>>> requestCaptor;

    private IElasticsearchIndexService indexService;

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH);
        ElasticsearchIndexService baseService = new ElasticsearchIndexService(esClient, INDEX_NAME);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        MethodValidationInterceptor interceptor = new MethodValidationInterceptor(factory.getValidator());
        ProxyFactory proxyFactory = new ProxyFactory(baseService);
        proxyFactory.addAdvice(interceptor);
        indexService = (IElasticsearchIndexService) proxyFactory.getProxy();
    }

    @Nested
    @DisplayName("Given a new document to index")
    class NewDocumentIndexing {

        @BeforeEach
        void setUpForNewDocument() throws IOException {
            when(indexResponse.result()).thenReturn(Result.Created);
            when(esClient.index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>any()))
                    .thenReturn(indexResponse);
        }

        @Test
        @DisplayName("When indexing, then correct index name should be used")
        void whenIndexing_thenCorrectIndexNameShouldBeUsed() throws Exception {
            indexService.indexDocument(DOCUMENT_ID, FILENAME, OCR_TEXT);
            IndexRequest<Map<String, Object>> request = captureIndexRequest();
            assertThat(request.index()).isEqualTo(INDEX_NAME);
        }

        @Test
        @DisplayName("When indexing, then document ID should be set correctly")
        void whenIndexing_thenDocumentIdShouldBeSetCorrectly() throws Exception {
            indexService.indexDocument(DOCUMENT_ID, FILENAME, OCR_TEXT);
            IndexRequest<Map<String, Object>> request = captureIndexRequest();
            assertThat(request.id()).isEqualTo(DOCUMENT_ID);
        }

        @Test
        @DisplayName("When indexing, then all required fields should be present")
        void whenIndexing_thenAllRequiredFieldsShouldBePresent() throws Exception {
            indexService.indexDocument(DOCUMENT_ID, FILENAME, OCR_TEXT);
            Map<String, Object> document = captureIndexRequest().document();
            assertThat(document)
                    .containsEntry(KEY_DOCUMENT_ID, DOCUMENT_ID)
                    .containsEntry(KEY_FILENAME, FILENAME)
                    .containsEntry(KEY_OCR_TEXT, OCR_TEXT)
                    .containsKey(TIMESTAMP_FIELD);
        }

        @Test
        @DisplayName("When indexing, then exactly one indexing operation should occur")
        void whenIndexing_thenExactlyOneOperationShouldOccur() throws Exception {
            indexService.indexDocument(DOCUMENT_ID, FILENAME, OCR_TEXT);
            verify(esClient, times(1))
                    .index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>any());
            verifyNoMoreInteractions(esClient);
        }
    }

    @Nested
    @DisplayName("Given an existing document")
    class ExistingDocumentUpdate {

        @BeforeEach
        void setUpForExistingDocument() throws IOException {
            when(indexResponse.result()).thenReturn(Result.Updated);
            when(esClient.index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>any()))
                    .thenReturn(indexResponse);
        }

        @Test
        @DisplayName("When document exists, then update should be handled correctly")
        void whenDocumentExists_thenUpdateShouldBeHandledCorrectly() throws Exception {
            indexService.indexDocument(DOCUMENT_ID, FILENAME, OCR_TEXT);
            verify(esClient, times(1))
                    .index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>any());
            verifyNoMoreInteractions(esClient);
        }
    }

    @Nested
    @DisplayName("Given a failure occurs during indexing")
    class IndexingFailure {

        @BeforeEach
        void setUpForIndexingFailure() throws IOException {
            IOException simulatedIOException = new IOException("Simulated indexing failure");
            when(esClient.index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>any()))
                    .thenThrow(simulatedIOException);
        }

        @Test
        @DisplayName("When ElasticsearchClient throws IOException, it is logged and rethrown")
        void whenIndexFails_thenIOExceptionIsRethrown() throws Exception {
            assertThrows(IOException.class, () ->
                    indexService.indexDocument(DOCUMENT_ID, FILENAME, OCR_TEXT));
            verify(esClient, times(1))
                    .index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>any());
            verifyNoMoreInteractions(esClient);
        }
    }

    private IndexRequest<Map<String, Object>> captureIndexRequest() throws IOException {
        verify(esClient).index(requestCaptor.capture());
        return requestCaptor.getValue();
    }
}