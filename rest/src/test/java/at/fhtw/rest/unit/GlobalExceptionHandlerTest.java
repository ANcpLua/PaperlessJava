package at.fhtw.rest.unit;

import at.fhtw.rest.api.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private static final String INVALID_INPUT_MESSAGE = "Invalid input provided";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
    private static final String ERROR_KEY = "error";

    @Nested
    @DisplayName("handleIllegalArgument Tests")
    class IllegalArgumentTests {

        @Test
        @DisplayName("should return BAD_REQUEST with proper error message")
        void testHandleIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException(INVALID_INPUT_MESSAGE);

            ResponseEntity<?> response = handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isInstanceOf(Map.class);
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertThat(body.get(ERROR_KEY)).isEqualTo(INVALID_INPUT_MESSAGE);
        }
    }

    @Nested
    @DisplayName("handleIOException Tests")
    class IOExceptionTests {

        @Test
        @DisplayName("should return INTERNAL_SERVER_ERROR with generic error message for IOException")
        void testHandleIOException() {
            IOException ex = new IOException("File system error");

            ResponseEntity<?> response = handler.handleIOException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isInstanceOf(Map.class);
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertThat(body.get(ERROR_KEY)).isEqualTo(INTERNAL_SERVER_ERROR_MESSAGE);
        }
    }

    @Nested
    @DisplayName("handleException Tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return INTERNAL_SERVER_ERROR with generic error message for generic Exception")
        void testHandleException() {
            Exception ex = new Exception("Unexpected error");

            ResponseEntity<?> response = handler.handleException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isInstanceOf(Map.class);
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertThat(body.get(ERROR_KEY)).isEqualTo(UNEXPECTED_ERROR_MESSAGE);
        }
    }
}