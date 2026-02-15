package com.project.aggregator.controller;

import com.project.aggregator.mapper.AggregatorMapper;
import com.project.aggregator.model.ProductRequest;
import com.project.aggregator.model.ProductResponse;
import com.project.aggregator.service.ProductFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Single REST endpoint for the Product Information Aggregator.
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
@RequiredArgsConstructor
public class AggregatorController {

    private final ProductFacade productFacade;
    private final AggregatorMapper aggregatorMapper;

    @GetMapping(value = "/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> getProduct(@PathVariable @NotBlank String productId,
                                                      @Valid @ModelAttribute ProductRequest request) {
        var productCommand = aggregatorMapper.toCommand(productId, request);
        var response = productFacade.aggregate(productCommand);
        return ResponseEntity.ok(response);
    }
}
