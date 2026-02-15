package com.project.aggregator.model;

import java.time.Instant;

public record AvailabilityDto(Integer stock, String warehouse, Instant expectedDelivery) implements FetchResult {

    public static final String RESPONSE_TYPE = AvailabilityDto.class.getSimpleName();

    @Override
    public String getName() {
        return RESPONSE_TYPE;
    }
}
