package com.project.aggregator.model;

import java.util.List;
import java.util.Map;

public record CatalogDto(
        String productId,
        String name,
        String description,
        Map<String, String> specs,
        List<String> images)
{
}
