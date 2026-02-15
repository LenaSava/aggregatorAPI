package com.project.aggregator.mapper;

import com.project.aggregator.model.*;

public interface ProductResponseMapper {

    static ProductResponse toDto(CatalogDto catalogDto, AvailabilityDto availabilityDto, PricingDto pricingDto, CustomerDto customerDto) {
        return ProductResponse.builder()
                .catalog(toDto(catalogDto))
                .availability(toDto(availabilityDto))
                .price(toDto(pricingDto))
                .customer(toDto(customerDto))
                .build();
    }

    private static ProductResponse.CatalogResponse toDto(CatalogDto catalogDto) {
        if (catalogDto == null || catalogDto.name()  == null) {
            return null;
        }
        return ProductResponse.CatalogResponse.builder()
                .productId(catalogDto.productId())
                .name(catalogDto.name())
                .description(catalogDto.description())
                .image(catalogDto.images())
                .specs(catalogDto.specs())
                .build();
    }

    private static ProductResponse.AvailabilityResponse toDto(AvailabilityDto availabilityDto) {
        if (availabilityDto == null || availabilityDto.getName() == null) {
            return ProductResponse.AvailabilityResponse.builder().status(ProductResponse.AvailabilityResponse.Status.UNKNOWN_STOCK).build();
        }
        return ProductResponse.AvailabilityResponse.builder()
                .stock(availabilityDto.stock())
                .expectedDeliver(availabilityDto.expectedDelivery())
                .warehouse(availabilityDto.warehouse())
                .status(ProductResponse.AvailabilityResponse.Status.IN_STOCK)
                .build();
    }

    private static ProductResponse.PriceResponse toDto(PricingDto pricingDto) {
        if (pricingDto == null || pricingDto.basePrice() == null) {
            return ProductResponse.PriceResponse.builder().status(ProductResponse.PriceResponse.Status.UNAVAILABLE).build();
        }
        return ProductResponse.PriceResponse.builder()
                .basePrice(pricingDto.basePrice())
                .discount(pricingDto.discount())
                .finalPrice(pricingDto.finalPrice())
                .status(ProductResponse.PriceResponse.Status.AVAILABLE)
                .build();
    }

    private static ProductResponse.CustomerResponse toDto(CustomerDto customerDto) {
        if (customerDto == null) {
            return ProductResponse.CustomerResponse.builder().status(ProductResponse.CustomerResponse.Status.NON_PERSONALIZED).build();
        }
        return ProductResponse.CustomerResponse.builder()
                .customerId(customerDto.customerId())
                .preference(customerDto.preferences())
                .segment(customerDto.segment())
                .status(ProductResponse.CustomerResponse.Status.PERSONALIZED)
                .build();
    }
}
