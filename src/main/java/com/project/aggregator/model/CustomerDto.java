package com.project.aggregator.model;

import java.util.List;

public record CustomerDto(String customerId, String segment, List<String> preferences) implements FetchResult {

    public static final String RESPONSE_TYPE = CustomerDto.class.getSimpleName();

    @Override
    public String getName() {
        return RESPONSE_TYPE;
    }
}
