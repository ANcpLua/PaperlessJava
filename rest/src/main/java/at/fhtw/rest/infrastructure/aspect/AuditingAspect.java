package at.fhtw.rest.infrastructure.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * <p>
 * This aspect intercepts all method calls within the REST package and logs detailed [REQUEST],
 * [RESPONSE], and [ERROR] messages, including method names, arguments, execution durations, and results.
 * </p>
 *
 * <p>
 * For more information on Spring AOP, please refer to:
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop">Spring AOP Documentation</a>.
 * </p>
 */

@Slf4j
@Aspect
@Component
public class AuditingAspect {

    @Pointcut("execution(* at.fhtw.rest..*(..))")
    public void allRestMethods() {
    }

    @Around("allRestMethods()")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String argsString = Arrays.deepToString(joinPoint.getArgs());
        log.info(">> [REQUEST] Method {} invoked with arguments: {}", methodName, argsString);
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("<< [RESPONSE] Method {} completed successfully in {} ms with result: {}",
                    methodName, duration, result);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("!! [ERROR] Method {} failed in {} ms with arguments: {}. Exception: {}",
                    methodName, duration, argsString, ex.getMessage(), ex);
            throw ex;
        }
    }
}