package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.stockasticappbackend.dto.stock.StockRequest;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.model.entity.Stock;

/**
 * MapStruct mapper for stock operations.
 * Handles conversions between Stock entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockMapper {

    /**
     * Converts a StockRequest DTO to a Stock entity.
     *
     * @param stockRequest The StockRequest DTO.
     * @return The mapped Stock entity.
     */
    @Mapping(target = "stockId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "image", ignore = true)
    Stock toEntity(StockRequest stockRequest);

    /**
     * Converts a Stock entity to a StockResponse DTO.
     *
     * @param stock The Stock entity.
     * @return The mapped StockResponse.
     */
    @Mapping(target = "currentPrice", ignore = true)
    @Mapping(target = "volume", ignore = true)
    @Mapping(target = "changePercent", ignore = true)
    StockResponse toResponse(Stock stock);

    /**
     * Converts a list of Stock entities to StockResponse DTOs.
     *
     * @param stocks The list of Stock entities.
     * @return The list of mapped StockResponse objects.
     */
    List<StockResponse> toResponseList(List<Stock> stocks);

    /**
     * Updates an existing Stock entity from a StockRequest DTO.
     *
     * @param stockRequest The source StockRequest DTO.
     * @param stock        The target Stock entity to update.
     */
    @Mapping(target = "stockId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "image", ignore = true)
    void updateEntityFromRequest(StockRequest stockRequest, @MappingTarget Stock stock);
}