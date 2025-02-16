package at.fhtw.services.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class AuditingAspect {
    @Around("execution(* at.fhtw.services..*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] Entering {} with arguments: {}", methodName, Arrays.toString(args));
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[RESPONSE] Exiting {} with result: {} (Execution time: {} ms)", methodName, result, duration);
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[ERROR] Exception in {} with arguments: {} after {} ms. Error: {}",
                    methodName, Arrays.toString(args), duration, t.getMessage(), t);
            throw t;
        }
    }
}