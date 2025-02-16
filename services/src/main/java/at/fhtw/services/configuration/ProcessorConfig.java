package at.fhtw.services.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.minio.MinioClient;
import net.sourceforge.tess4j.Tesseract;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

@Configuration
public class ProcessorConfig {
    @Bean
    public Tesseract tesseract(
            @Value("${tesseract.data-path:}") String configuredPath,
            @Value("${tesseract.language:eng}") String language,
            @Value("${tesseract.dpi:300}") int dpi
    ) {
        String validPath = findTessdataPath(configuredPath);
        Tesseract t = new Tesseract();
        t.setDatapath(validPath);
        t.setLanguage(language);
        t.setVariable("dpi", String.valueOf(dpi));
        t.setOcrEngineMode(1);
        t.setPageSegMode(3);
        return t;
    }

    public String findTessdataPath(String configuredPath) {
        if (isValidTessdataPath(configuredPath)) return configuredPath;
        String userDir = System.getProperty("user.dir");
        List<String> fallbackPaths = List.of(
                new File(userDir, "tessdata").getAbsolutePath(),
                new File(new File(userDir).getParent(), "tessdata").getAbsolutePath(),
                "/usr/share/tesseract-ocr/tessdata",
                "/usr/share/tesseract-ocr/5.00/tessdata",
                "/usr/local/share/tessdata"
        );
        return fallbackPaths.stream()
                .filter(this::isValidTessdataPath)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find valid tessdata directory with eng.traineddata."
                ));
    }

    public boolean isValidTessdataPath(String path) {
        if (path == null || path.isBlank()) return false;
        File dir = new File(path);
        return dir.isDirectory() && new File(dir, "eng.traineddata").exists();
    }

    @Bean
    public MinioClient minioClient(
            @Value("${minio.url:http://minio:9000}") String endpoint,
            @Value("${minio.access-key:paperless}") String accessKey,
            @Value("${minio.secret-key:paperless}") String secretKey
    ) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    @Bean
    public TopicExchange documentExchange(@Value("${rabbitmq.exchange:document_exchange}") String exchangeName) {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue processingQueue(@Value("${rabbitmq.queue.processing:document_processing_queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue indexingQueue(@Value("${rabbitmq.queue.indexing:document_indexing_queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue resultQueue(@Value("${rabbitmq.queue.result:document_result_queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding bindProcessingQueue(
            @Qualifier("processingQueue") Queue processingQueue,
            TopicExchange documentExchange,
            @Value("${rabbitmq.routing-key.processing:document_routing_key}") String routingKey
    ) {
        return BindingBuilder.bind(processingQueue).to(documentExchange).with(routingKey);
    }

    @Bean
    public Binding bindIndexingQueue(
            @Qualifier("indexingQueue") Queue indexingQueue,
            TopicExchange documentExchange,
            @Value("${rabbitmq.routing-key.indexing:document_indexing_key}") String routingKey
    ) {
        return BindingBuilder.bind(indexingQueue).to(documentExchange).with(routingKey);
    }

    @Bean
    public Binding bindResultQueue(
            @Qualifier("resultQueue") Queue resultQueue,
            TopicExchange documentExchange,
            @Value("${rabbitmq.routing-key.result:document_result_key}") String routingKey
    ) {
        return BindingBuilder.bind(resultQueue).to(documentExchange).with(routingKey);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(
            @Value("${spring.elasticsearch.uris:http://elasticsearch:9200}") String esUri
    ) {
        RestClient restClient = RestClient.builder(HttpHost.create(esUri)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}