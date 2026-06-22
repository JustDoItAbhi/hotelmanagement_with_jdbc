package com.hotel_service.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Aspect
@Component
public class ProcessingAspect {

    private static final AtomicLong totalRequest=new AtomicLong();
    private static final AtomicLong failedRequests = new AtomicLong();
    private static final AtomicLong activeRequests = new AtomicLong();
    @Around("@annotation(tracking)")
    public Object processingTime(
            ProceedingJoinPoint joinPoint,
            Tracking tracking) throws Throwable {

        activeRequests.incrementAndGet();
        totalRequest.incrementAndGet();

        long startTime = System.nanoTime();

        try {

            return joinPoint.proceed();

        } catch (Exception e) {

            failedRequests.incrementAndGet();
            throw e;

        } finally {

            activeRequests.decrementAndGet();

            long responseTime =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startTime);

            double errorPercentage =
                    totalRequest.get() == 0 ? 0 :
                            ((double) failedRequests.get()
                                    / totalRequest.get()) * 100;

            ProcessingMatrics metrics =
                    ProcessingMatrics.builder()
                            .apiName(joinPoint.getSignature().getName())
                            .apiResponseTime(responseTime)
                            .memoryUsage(getMemoryUsage())
                            .numberOfActiveUsers(activeRequests.get())
                            .errorTime(errorPercentage)
                            .build();

            metrics.print();
        }
    }
    private String getMemoryUsage(){
        Runtime runTime=Runtime.getRuntime();
        long usedMemory=(runTime.totalMemory()- runTime.freeMemory())/(1024*1024);
        return usedMemory+ " MB";
    }
}
