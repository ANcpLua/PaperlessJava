package at.fhtw.services.integration;

import at.fhtw.services.ElasticsearchIndexServiceImp;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static at.fhtw.services.integration.IntegrationTestBase.ElasticsearchConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(IntegrationTestBase.SharedContainersExtension.class)
class ElasticsearchIndexServiceImpIntegrationTest extends IntegrationTestBase {


    private ElasticsearchClient esClient;
    private ElasticsearchIndexServiceImp indexService;
    private RestClient restClient;

    @BeforeAll
    public void setUp() {
        String elasticsearchUrl = SharedContainersExtension.elasticsearchContainer.getHttpHostAddress();
        restClient = RestClient.builder(HttpHost.create(elasticsearchUrl)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);
        indexService = new ElasticsearchIndexServiceImp(esClient, INDEX_NAME);
    }

    @BeforeEach
    public void cleanIndex() {
        try {
            if (Boolean.TRUE.equals(esClient.indices().exists(e -> e.index(INDEX_NAME)).value())) {
                esClient.indices().delete(d -> d.index(INDEX_NAME));
            }
        } catch (Exception ignored) {
        }
    }

    @AfterAll
    public void tearDown() throws IOException {
        try {
            if (Boolean.TRUE.equals(esClient.indices().exists(e -> e.index(INDEX_NAME)).value())) {
                esClient.indices().delete(d -> d.index(INDEX_NAME));
            }
        } catch (Exception ignored) {
        } finally {
            restClient.close();
        }
    }

    @Test
    public void testIndexDocumentSuccess() throws IOException {
        indexService.indexDocument(TEST_DOC_ID_SUCCESS, TEST_FILENAME_SUCCESS, TEST_OCR_TEXT_SUCCESS);
        esClient.indices().refresh(r -> r.index(INDEX_NAME));

        var getResponse = esClient.get(g -> g.index(INDEX_NAME).id(TEST_DOC_ID_SUCCESS), Map.class);
        assertThat(MSG_DOC_FOUND, getResponse.found(), is(true));

        var document = getResponse.source();
        assertThat(MSG_DOC_SOURCE_NOT_NULL, document, notNullValue());
        assertThat(document.get(FIELD_DOCUMENT_ID), is(TEST_DOC_ID_SUCCESS));
        assertThat(document.get(FIELD_FILENAME), is(TEST_FILENAME_SUCCESS));
        assertThat(document.get(FIELD_OCR_TEXT), is(TEST_OCR_TEXT_SUCCESS));
        assertThat(MSG_TIMESTAMP_PRESENT, document.get(FIELD_TIMESTAMP), notNullValue());
    }

    @Test
    public void testIndexDocumentUpdate() throws IOException {
        indexService.indexDocument(TEST_DOC_ID_UPDATE, ORIGINAL_FILENAME, ORIGINAL_OCR_TEXT);
        esClient.indices().refresh(r -> r.index(INDEX_NAME));

        indexService.indexDocument(TEST_DOC_ID_UPDATE, UPDATED_FILENAME, UPDATED_OCR_TEXT);
        esClient.indices().refresh(r -> r.index(INDEX_NAME));

        var getResponse = esClient.get(g -> g.index(INDEX_NAME).id(TEST_DOC_ID_UPDATE), Map.class);
        assertThat(MSG_DOC_FOUND_AFTER_UPDATE, getResponse.found(), is(true));

        var document = getResponse.source();
        assertThat(MSG_DOC_SOURCE_NOT_NULL, document, notNullValue());
        assertThat(document.get(FIELD_DOCUMENT_ID), is(TEST_DOC_ID_UPDATE));
        assertThat(document.get(FIELD_FILENAME), is(UPDATED_FILENAME));
        assertThat(document.get(FIELD_OCR_TEXT), is(UPDATED_OCR_TEXT));
    }
}