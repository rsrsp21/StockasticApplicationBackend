package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stockasticappbackend.dto.order.HoldingsResponse;
import com.stockasticappbackend.model.entity.Holdings;

/**
 * MapStruct mapper for Holdings operations.
 * Handles conversions between Holdings entities and DTOs.
 * Note: currentPrice, currentValue, profitLoss fields require runtime data
 * and must be set manually after mapping.
 */
@Mapper(componentModel = "spring")
public interface HoldingsMapper {

    /**
     * Converts a Holdings entity to a HoldingsResponse DTO.
     * Note: currentPrice, currentValue, profitLoss, profitLossPercent
     * must be set manually using setMarketData() after this mapping.
     *
     * @param holdings The Holdings entity.
     * @return The mapped HoldingsResponse (partial, needs market data).
     */
    @Mapping(target = "stockId", source = "stock.stockId")
    @Mapping(target = "symbol", source = "stock.symbol")
    @Mapping(target = "stockName", source = "stock.name")
    @Mapping(target = "image", source = "stock.image")
    @Mapping(target = "sector", source = "stock.sector")
    @Mapping(target = "investedAmount", ignore = true) // Calculated in Service
    @Mapping(target = "realizedPnl", source = "totalRealizedPnl")
    @Mapping(target = "currentPrice", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "profitLoss", ignore = true)
    @Mapping(target = "profitLossPercent", ignore = true)
    @Mapping(target = "totalPnl", ignore = true)
    @Mapping(target = "dayChange", ignore = true)
    @Mapping(target = "dayChangePercent", ignore = true)
    HoldingsResponse toResponse(Holdings holdings);

    /**
     * Converts a list of Holdings entities to HoldingsResponse DTOs.
     *
     * @param holdings The list of Holdings entities.
     * @return The list of mapped HoldingsResponse objects.
     */
    List<HoldingsResponse> toResponseList(List<Holdings> holdings);
}
