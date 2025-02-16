package at.fhtw.services.unit;

import org.json.JSONObject;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class TestBase {
    public static class DocumentConstants {
        public static final String VALID_DOCUMENT_ID = "doc123";
        public static final String VALID_FILENAME = "HelloWorld.pdf";
        public static final String VALID_EXTRACTED_TEXT = "Extracted text";
        public static final String JSON_KEY_DOCUMENT_ID = "documentId";
        public static final String JSON_KEY_FILENAME = "filename";
    }

    public static class MinioConstants {
        public static final String MOCK_CONTENT = "Hello, world!";
        public static final String DOCUMENT_ID = "myDocument";
        public static final String EXTENSION_TXT = ".txt";
        public static final String BUCKET_NAME = "documents";
        public static final String MINIO_ERROR_MESSAGE = "Minio error";
        public static final String ASSERT_EXCEPTION_MESSAGE = "Exception message should match";
        public static final String ASSERT_FILE_EXISTS = "Downloaded file should exist";
        public static final String ASSERT_FILE_NAME_MATCHES = "Downloaded file name should match expected";
        public static final String ASSERT_FILE_CONTENT_MATCHES = "File content should match expected";
    }

    public static class ElasticsearchConstants {
        public static final String INDEX_NAME = "documents";
        public static final String FILENAME = "file.pdf";
        public static final String OCR_TEXT = "sample ocr text";
        public static final String TIMESTAMP_FIELD = "@timestamp";
        public static final String KEY_DOCUMENT_ID = "documentId";
        public static final String KEY_FILENAME = "filename";
        public static final String KEY_OCR_TEXT = "ocrText";
    }

    public static class MessageBrokerConstants {
        public static final String QUEUE_NAME = "resultQueue";
        public static final String FIELD_RESULT_QUEUE = "resultQueue";
        public static final String LONG_DOCUMENT_ID = new String(new char[1000]).replace("\0", "A");
        public static final String EXTRACTED_TEXT = "extracted text";
        public static final String JSON_KEY_DOCUMENT_ID = "documentId";
        public static final String JSON_KEY_OCR_TEXT = "ocrText";
        static final String DOC_1 = "doc1";
        static final String HELLO_WORLD = "Hello World";
        static final String SPECIAL_CHARS = "特殊字符";
        static final String DOC_WITH_SPACES = "doc with spaces";
        static final String TEXT_WITH_SPACES = "text with spaces";
        static final String DOC_ID = "docId";
        static final String OCR_TEXT = "ocrText";
        static final String SIMULATED_AMQP_ERROR = "Simulated AMQP error";
        static final String SIMULATED_JSON_ERROR = "Simulated JSON error";
    }

    public static class OcrConstants {
        public static final String DELETE_FAILED_PREFIX = "Failed to delete file ";
        public static final String MOCK_TEXT = "MOCK_TEXT";
        public static final String OCR_FAILED_MESSAGE = "OCR failed";
    }

    public static class ProcessorConfigTestConstants {
        public static final String TRAINED_DATA = "eng.traineddata";
        public static final String INVALID_TESSDATA_PATH = "invalid";
        public static final String ERROR_MESSAGE = "Could not find valid tessdata directory with eng.traineddata";
    }

    protected String createValidDocumentMessage() {
        JSONObject json = new JSONObject();
        json.put(DocumentConstants.JSON_KEY_DOCUMENT_ID, DocumentConstants.VALID_DOCUMENT_ID);
        json.put(DocumentConstants.JSON_KEY_FILENAME, DocumentConstants.VALID_FILENAME);
        return json.toString();
    }

    protected void assertFileDeleted(File file) {
        assertThat(file.exists())
                .as("Temporary file [%s] should be deleted", file.getAbsolutePath())
                .isFalse();
    }
}