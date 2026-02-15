package com.project.aggregator.client;

import com.project.aggregator.model.CatalogDto;

public interface CatalogClient {

    CatalogDto fetch(String productId, String market);
}
