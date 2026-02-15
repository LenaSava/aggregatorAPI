package com.project.aggregator.service;

import com.project.aggregator.client.AvailabilityClient;
import com.project.aggregator.model.AvailabilityDto;
import com.project.aggregator.model.ProductCommand;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService implements FetchService<AvailabilityDto> {

    private final AvailabilityClient client;

    @Value("${app.timeout.service-timeout-millis}")
    private long timeoutMillis;

    @Override
    @CircuitBreaker(name = "availabilityCB", fallbackMethod = "fallback")
    public CompletableFuture<AvailabilityDto> fetch(ProductCommand command) {
        StopWatch sw = new StopWatch();
        sw.start();
        log.info("Executing Availability info");
        return CompletableFuture
                .supplyAsync(() -> client.fetch(command.productId(), command.market()))
                .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .whenComplete((r, e) -> {
                    sw.stop();
                    log.info("Executed Availability info after  {} ms", sw.getTotalTimeMillis());
                });
    }

    private CompletableFuture<AvailabilityDto> fallback(ProductCommand command, Throwable throwable) {
        log.error("Unable to execute call. Return default result", throwable);
        return CompletableFuture.completedFuture(new AvailabilityDto(null, null, null));
    }
}
