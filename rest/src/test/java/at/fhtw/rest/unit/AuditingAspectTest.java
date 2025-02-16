package at.fhtw.rest.unit;

import at.fhtw.rest.infrastructure.aspect.AuditingAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class AuditingAspectTest {

    private static final String SOME_METHOD_SHORT_STRING = "someMethod()";
    private static final String SUCCESSFUL_RESULT = "successful result";
    private static final String SIMULATED_EXCEPTION_MESSAGE = "Simulated exception";

    private AuditingAspect auditingAspect;

    @BeforeEach
    void init() {
        auditingAspect = new AuditingAspect();
    }

    abstract static class WithJoinPointSetup {
        protected ProceedingJoinPoint joinPoint;

        @BeforeEach
        void setupJoinPoint() {
            joinPoint = mock(ProceedingJoinPoint.class);
            Signature signature = mock(Signature.class);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.toShortString()).thenReturn(SOME_METHOD_SHORT_STRING);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 2, true});
        }
    }

    @Nested
    @DisplayName("Happy Path: Successful Audit")
    class AuditSuccessTests extends WithJoinPointSetup {
        @Test
        @DisplayName("Returns result from joinPoint.proceed()")
        void returnsResult() throws Throwable {
            when(joinPoint.proceed()).thenReturn(SUCCESSFUL_RESULT);

            Object result = auditingAspect.audit(joinPoint);

            assertThat(result).isEqualTo(SUCCESSFUL_RESULT);
            verify(joinPoint).proceed();
        }
    }

    @Nested
    @DisplayName("Exception Path: Audit Failure")
    class AuditExceptionTests extends WithJoinPointSetup {
        @Test
        @DisplayName("Rethrows exception from joinPoint.proceed()")
        void rethrowsException() throws Throwable {
            Exception exception = new Exception(SIMULATED_EXCEPTION_MESSAGE);
            when(joinPoint.proceed()).thenThrow(exception);

            assertThatThrownBy(() -> auditingAspect.audit(joinPoint))
                    .isInstanceOf(Exception.class)
                    .hasMessage(SIMULATED_EXCEPTION_MESSAGE);
            verify(joinPoint).proceed();
        }
    }

    @Nested
    @DisplayName("Pointcut Verification")
    class PointcutTests {
        @Test
        @DisplayName("allRestMethods() executes without exceptions")
        void executesWithoutExceptions() {
            assertThatCode(() -> auditingAspect.allRestMethods())
                    .doesNotThrowAnyException();
        }
    }
}