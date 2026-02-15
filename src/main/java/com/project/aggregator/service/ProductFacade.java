package com.project.aggregator.service;

import com.project.aggregator.mapper.ProductResponseMapper;
import com.project.aggregator.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final CatalogService catalogService;
    private final AggregatorService aggregatorService;

    public ProductResponse aggregate(ProductCommand productCommand) {

        // 1) Critical: must succeed or throw
        var catalogDto = catalogService.fetch(productCommand.productId(), productCommand.market());

        // 2) Fan out all optional services in parallel, collect results
        var results = aggregatorService.fetch(productCommand);

        // 3) Build response from catalog base + optional enrichments
        return ProductResponseMapper.toDto(
                catalogDto,
                getValue(results, AvailabilityDto.RESPONSE_TYPE, AvailabilityDto.class),
                getValue(results, PricingDto.RESPONSE_TYPE, PricingDto.class),
                getValue(results, CustomerDto.RESPONSE_TYPE, CustomerDto.class));

    }

    private <T extends FetchResult> T getValue(Map<String, FetchResult> results, String key, Class<T> type) {
        return Optional.ofNullable(results.get(key))
                .filter(type::isInstance)
                .map(type::cast)
                .orElse(null);
    }
}
