package at.fhtw.services.integration;

import at.fhtw.services.aspect.AuditingAspect;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static at.fhtw.services.integration.IntegrationTestBase.LoggingConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class AuditingAspectIntegrationTest extends IntegrationTestBase {

    private ListAppender<ILoggingEvent> listAppender;

    @Autowired
    private DummyAspect dummyService;

    @BeforeEach
    public void setUp() {
        Logger aspectLogger = (Logger) LoggerFactory.getLogger(AuditingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
    }

    @Test
    public void testAuditingAspectLogs() {
        String result = dummyService.dummyMethod();
        assertThat(result).isEqualTo(EXPECTED_RESULT);

        boolean hasRequestLog = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(LOG_REQUEST));

        boolean hasResponseLog = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(LOG_RESPONSE));

        assertThat(hasRequestLog)
                .as(EXPECTED_REQUEST_MESSAGE)
                .isTrue();
        assertThat(hasResponseLog)
                .as(EXPECTED_RESPONSE_MESSAGE)
                .isTrue();
    }

    @Test
    public void testAuditingAspectErrorLogs() {
        try {
            dummyService.dummyErrorMethod();
            fail(EXPECTED_EXCEPTION_FAIL_MESSAGE);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).isEqualTo(EXPECTED_EXCEPTION_MESSAGE);
        }

        boolean hasRequestLog = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(LOG_REQUEST));

        boolean hasErrorLog = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(LOG_ERROR));

        assertThat(hasRequestLog)
                .as(EXPECTED_REQUEST_MESSAGE)
                .isTrue();
        assertThat(hasErrorLog)
                .as(EXPECTED_ERROR_MESSAGE)
                .isTrue();
    }
}