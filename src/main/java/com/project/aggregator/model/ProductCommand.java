package com.project.aggregator.model;

public record ProductCommand(
        String productId,
        String market,
        String customerId
) {
}