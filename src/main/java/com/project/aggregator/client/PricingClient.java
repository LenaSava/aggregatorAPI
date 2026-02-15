package com.project.aggregator.client;

import com.project.aggregator.model.PricingDto;

public interface PricingClient {

    PricingDto fetch(String productId, String market, String customerId);
}
