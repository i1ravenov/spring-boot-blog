package com.boot.blog.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();

        log.info("ENTER {}", method);
        try {
            Object result = joinPoint.proceed();
            log.info("EXIT {} ({} ms)", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("ERROR in {}: {}", method, e.getMessage(), e);
            throw e;
        }
    }
}
