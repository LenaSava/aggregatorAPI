package com.project.aggregator.mapper;

import com.project.aggregator.model.ProductCommand;
import com.project.aggregator.model.ProductRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AggregatorMapper {

    default ProductCommand toCommand(String productId, ProductRequest productRequest) {
        return new ProductCommand(productId, productRequest.market(), productRequest.customerId());
    }
}
