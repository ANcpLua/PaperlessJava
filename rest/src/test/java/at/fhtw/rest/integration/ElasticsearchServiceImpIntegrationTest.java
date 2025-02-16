package at.fhtw.rest.integration;

import at.fhtw.rest.core.ElasticsearchServiceImp;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Elasticsearch Service Integration Tests")
class ElasticsearchServiceImpIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(ElasticsearchServiceImpIntegrationTest.class.getName());
    private static final String ES_DOCKER_VERSION = "8.17.0";
    private static final String ES_DOCKER_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:" + ES_DOCKER_VERSION;
    private static final String ES_INDEX_NAME = "documents";
    private static final String ES_FIELD_DOCUMENT_ID = "documentId";
    private static final String ES_FIELD_FILENAME = "filename";
    private static final String ES_FIELD_OCR_TEXT = "ocrText";
    private static final String TEST_DOCUMENT_ID = "test-doc-1";
    private static final String TEST_ORIGINAL_FILENAME = "test.pdf";
    private static final String TEST_UPDATED_FILENAME = "updated-test.pdf";
    private static final String TEST_OCR_CONTENT = "This is a test PDF document for OCR processing";

    private ElasticsearchClient esClient;
    private ElasticsearchServiceImp elasticsearchServiceImp;
    private RestClient restClient;

    @Container
    private static final ElasticsearchContainer ES_CONTAINER = new ElasticsearchContainer(
            DockerImageName.parse(ES_DOCKER_IMAGE))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false");

    @BeforeAll
    void setUpAll() {
        String elasticsearchUrl = ES_CONTAINER.getHttpHostAddress();
        restClient = RestClient.builder(HttpHost.create(elasticsearchUrl)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);
        elasticsearchServiceImp = new ElasticsearchServiceImp(esClient, ES_INDEX_NAME);
    }

    @BeforeEach
    void cleanIndex() {
        try {
            boolean indexExists = Boolean.TRUE.equals(esClient.indices().exists(e -> e.index(ES_INDEX_NAME)).value());
            if (indexExists) {
                esClient.indices().delete(d -> d.index(ES_INDEX_NAME));
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to clean index before test: " + e.getMessage());
        }
    }

    @AfterAll
    void tearDownAll() {
        try {
            boolean indexExists = Boolean.TRUE.equals(esClient.indices().exists(e -> e.index(ES_INDEX_NAME)).value());
            if (indexExists) {
                esClient.indices().delete(d -> d.index(ES_INDEX_NAME));
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to delete index during teardown: " + e.getMessage());
        } finally {
            try {
                restClient.close();
            } catch (IOException e) {
                LOGGER.warning("Failed to close REST client: " + e.getMessage());
            }
        }
    }

    private Map<String, Object> createTestDocument() {
        Map<String, Object> document = new HashMap<>();
        document.put(ES_FIELD_DOCUMENT_ID, TEST_DOCUMENT_ID);
        document.put(ES_FIELD_FILENAME, TEST_ORIGINAL_FILENAME);
        document.put(ES_FIELD_OCR_TEXT, TEST_OCR_CONTENT);
        return document;
    }

    private void indexTestDocument(Map<String, Object> document) throws IOException {
        esClient.index(i -> i
                .index(ES_INDEX_NAME)
                .id(TEST_DOCUMENT_ID)
                .document(document)
        );
        esClient.indices().refresh(r -> r.index(ES_INDEX_NAME));
    }

    @Nested
    @DisplayName("Elasticsearch Index Operations")
    class IndexOperationTests {

        @Test
        @DisplayName("Should update filename successfully")
        void shouldUpdateFilename() throws IOException {
            Map<String, Object> document = createTestDocument();
            indexTestDocument(document);
            elasticsearchServiceImp.updateFilename(TEST_DOCUMENT_ID, TEST_UPDATED_FILENAME);
            var response = esClient.get(g -> g.index(ES_INDEX_NAME).id(TEST_DOCUMENT_ID), Map.class);
            assertThat("Document should be found after update", response.found(), is(true));
            var source = response.source();
            assertThat("Document source should not be null", source, is(notNullValue()));
            assertThat("Filename should be updated", source.get(ES_FIELD_FILENAME), is(equalTo(TEST_UPDATED_FILENAME)));
        }

        @Test
        @DisplayName("Should delete document successfully")
        void shouldDeleteDocument() throws IOException {
            Map<String, Object> document = createTestDocument();
            indexTestDocument(document);
            elasticsearchServiceImp.deleteDocument(TEST_DOCUMENT_ID);
            var response = esClient.get(g -> g.index(ES_INDEX_NAME).id(TEST_DOCUMENT_ID), Map.class);
            assertThat("Document should be deleted", response.found(), is(false));
        }

        @Test
        @DisplayName("Should find document successfully")
        void shouldFindDocumentSuccessfully() throws IOException {
            Map<String, Object> document = createTestDocument();
            indexTestDocument(document);
            List<String> results = elasticsearchServiceImp.searchIdsByQuery("PDF");
            assertThat("Should find one document", results.size(), is(1));
            assertThat("Should find the test document", results.get(0), is(equalTo(TEST_DOCUMENT_ID)));
        }
    }

    @Nested
    @DisplayName("Elasticsearch Query Operations")
    class QueryOperationTests {

        @Test
        @DisplayName("Should handle empty and null queries")
        void shouldHandleEmptyAndNullQueries() {
            assertThat("Null query should return empty results",
                    elasticsearchServiceImp.searchIdsByQuery(null), is(empty()));
        }

        @Test
        @DisplayName("Should handle search on non-existent index")
        void shouldHandleNonExistentIndex() throws IOException {
            boolean indexExists = Boolean.TRUE.equals(esClient.indices().exists(e -> e.index(ES_INDEX_NAME)).value());
            if (indexExists) {
                esClient.indices().delete(d -> d.index(ES_INDEX_NAME));
            }
            List<String> results = elasticsearchServiceImp.searchIdsByQuery("test");
            assertThat("Search on non-existent index should return empty results", results, is(empty()));
        }
    }
}