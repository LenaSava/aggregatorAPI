package com.project.aggregator.service;

import com.project.aggregator.client.CustomerClient;
import com.project.aggregator.model.CustomerDto;
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
public class CustomerService implements FetchService<CustomerDto> {

    private final CustomerClient client;
    @Value("${app.timeout.service-timeout-millis}")
    private long timeoutMillis;

    @Override
    @CircuitBreaker(name = "customerCB", fallbackMethod = "fallback")
    public CompletableFuture<CustomerDto> fetch(ProductCommand command) {
        StopWatch sw = new StopWatch();
        sw.start();
        log.info("Executing Customer info");
        return CompletableFuture.supplyAsync(() -> client.fetch(command.customerId()))
                .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .whenComplete((r, e) -> {
                    sw.stop();
                    log.info("Executed Customer info after  {} ms", sw.getTotalTimeMillis());
                });
    }

    private CompletableFuture<CustomerDto> fallback(ProductCommand command, Throwable throwable) {
        log.error("Unable to execute call. Return default result", throwable);
        return CompletableFuture.completedFuture(null);
    }
}
