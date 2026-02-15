package com.project.aggregator.service;

import com.project.aggregator.model.FetchResult;
import com.project.aggregator.model.ProductCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AggregatorService {

    private final List<FetchService<? extends FetchResult>> fetchServices;

    public Map<String, FetchResult> fetch(ProductCommand command) {
        List<CompletableFuture<FetchResult>> futures = fetchServices.stream()
                .map(service -> callSafely(service, command))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(FetchResult::getName, Function.identity()));
    }

    private CompletableFuture<FetchResult> callSafely(FetchService<? extends FetchResult> service, ProductCommand command) {
        try {
            return service.fetch(command)
                    .<FetchResult>thenApply(r -> r)
                    .exceptionally(ex -> {
                        log.warn("{} failed: {}", service.getClass().getSimpleName(), ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("{} threw: {}", service.getClass().getSimpleName(), ex.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}