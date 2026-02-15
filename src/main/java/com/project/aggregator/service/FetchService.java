package com.project.aggregator.service;

import com.project.aggregator.model.FetchResult;
import com.project.aggregator.model.ProductCommand;

import java.util.concurrent.CompletableFuture;

public interface FetchService<T extends FetchResult> {

    CompletableFuture<T> fetch(ProductCommand command);
}
