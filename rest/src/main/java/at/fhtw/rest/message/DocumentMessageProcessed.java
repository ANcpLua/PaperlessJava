package at.fhtw.rest.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class DocumentMessageProcessed {
    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("ocrText")
    private String ocrText;

    @JsonProperty("processedAt")
    private Instant processedAt;
}