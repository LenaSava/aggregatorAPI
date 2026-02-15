package com.project.aggregator.model;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder
@Data
public class ProductResponse {
    private CatalogResponse catalog;
    private PriceResponse price;
    private AvailabilityResponse availability;
    private CustomerResponse customer;

    @Builder
    @Data
    public static class CatalogResponse {
        private String productId;
        private String name;
        private String description;
        private Map<String, String> specs;
        private List<String> image;
    }

    @Builder
    @Data
    public static class PriceResponse {
        private BigDecimal basePrice;
        private BigDecimal discount;
        private BigDecimal finalPrice;
        private Status status;

        @Nullable
        public enum Status {
            AVAILABLE, UNAVAILABLE
        }
    }

    @Builder
    @Data
    public static class AvailabilityResponse {
        private Integer stock;
        private String warehouse;
        private Instant expectedDeliver;
        private Status status;

        public enum Status {
            IN_STOCK, UNKNOWN_STOCK
        }
    }

    @Builder
    @Data
    public static class CustomerResponse {
        private String customerId;
        private String segment;
        private List<String> preference;
        private Status status;

        public enum Status {
            PERSONALIZED, NON_PERSONALIZED
        }
    }
}
