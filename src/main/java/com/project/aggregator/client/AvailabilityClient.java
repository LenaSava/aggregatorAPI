package com.project.aggregator.client;

import com.project.aggregator.model.AvailabilityDto;

public interface AvailabilityClient {

    AvailabilityDto fetch(String productId, String market);
}
