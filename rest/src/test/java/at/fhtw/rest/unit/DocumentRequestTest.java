package at.fhtw.rest.unit;

import at.fhtw.rest.api.DocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DocumentRequestTest {

    private static ObjectMapper objectMapper;

    private static final String DOC_ID_1 = "doc001";
    private static final String FILENAME_1 = "example.txt";
    private static final long FILESIZE_1 = 1024L;
    private static final String FILETYPE_1 = "text/plain";
    private static final String OCR_TEXT_1 = "Extracted OCR text";
    private static final String DOC_ID_2 = "doc002";
    private static final String FILENAME_2 = "sample.pdf";
    private static final long FILESIZE_2 = 2048L;
    private static final String FILETYPE_2 = "application/pdf";
    private static final String OCR_TEXT_2 = "No OCR";

    @BeforeAll
    static void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Test getters and setters")
    public void testGettersAndSetters() {
        DocumentRequest request = new DocumentRequest();
        LocalDateTime now = LocalDateTime.now();
        request.setId(DOC_ID_1);
        request.setFilename(FILENAME_1);
        request.setFilesize(FILESIZE_1);
        request.setFiletype(FILETYPE_1);
        request.setUploadDate(now);
        request.setOcrJobDone(true);
        request.setOcrText(OCR_TEXT_1);
        assertThat(request.getId(), is(DOC_ID_1));
        assertThat(request.getFilename(), is(FILENAME_1));
        assertThat(request.getFilesize(), is(FILESIZE_1));
        assertThat(request.getFiletype(), is(FILETYPE_1));
        assertThat(request.getUploadDate(), is(now));
        assertThat(request.isOcrJobDone(), is(true));
        assertThat(request.getOcrText(), is(OCR_TEXT_1));
    }

    @Test
    @DisplayName("Test equals, hashCode and toString")
    public void testEqualsHashCodeToString() {
        LocalDateTime now = LocalDateTime.now();
        DocumentRequest req1 = new DocumentRequest(DOC_ID_1, FILENAME_1, FILESIZE_1, FILETYPE_1, now, true, OCR_TEXT_1);
        DocumentRequest req2 = new DocumentRequest(DOC_ID_1, FILENAME_1, FILESIZE_1, FILETYPE_1, now, true, OCR_TEXT_1);
        assertThat(req1, equalTo(req2));
        assertThat(req1.hashCode(), is(req2.hashCode()));
        assertThat(req1.toString(), containsString(DOC_ID_1));
    }

    @Test
    @DisplayName("Test JSON serialization and deserialization")
    public void testJsonSerializationDeserialization() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        DocumentRequest request = new DocumentRequest(DOC_ID_2, FILENAME_2, FILESIZE_2, FILETYPE_2, now, false, OCR_TEXT_2);
        String json = objectMapper.writeValueAsString(request);
        assertThat(json, containsString("\"id\":\"" + DOC_ID_2 + "\""));
        assertThat(json, containsString("\"filename\":\"" + FILENAME_2 + "\""));
        assertThat(json, containsString("\"filesize\":" + FILESIZE_2));
        assertThat(json, containsString("\"filetype\":\"" + FILETYPE_2 + "\""));
        assertThat(json, containsString("\"ocrJobDone\":false"));
        assertThat(json, containsString("\"ocrText\":\"" + OCR_TEXT_2 + "\""));
        DocumentRequest deserialized = objectMapper.readValue(json, DocumentRequest.class);
        assertThat(deserialized.getId(), is(DOC_ID_2));
        assertThat(deserialized.getFilename(), is(FILENAME_2));
        assertThat(deserialized.getFilesize(), is(FILESIZE_2));
        assertThat(deserialized.getFiletype(), is(FILETYPE_2));
        assertThat(deserialized.getUploadDate(), is(now));
        assertThat(deserialized.isOcrJobDone(), is(false));
        assertThat(deserialized.getOcrText(), is(OCR_TEXT_2));
    }
}