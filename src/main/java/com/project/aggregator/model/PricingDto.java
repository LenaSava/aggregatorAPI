package com.project.aggregator.model;

import java.math.BigDecimal;

public record PricingDto(BigDecimal basePrice, BigDecimal discount, BigDecimal finalPrice) implements FetchResult {
    public static final String RESPONSE_TYPE = PricingDto.class.getSimpleName();

    @Override
    public String getName() {
        return RESPONSE_TYPE;
    }
}
