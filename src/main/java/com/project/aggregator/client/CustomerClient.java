package com.project.aggregator.client;

import com.project.aggregator.model.CustomerDto;

public interface CustomerClient {

    CustomerDto fetch(String customerId);
}
