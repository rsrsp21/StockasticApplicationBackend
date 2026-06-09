package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.stockasticappbackend.dto.stockprice.StockPriceHistoryResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.model.entity.StockPrice;

/**
 * MapStruct mapper for stock price operations.
 * Handles conversions between StockPrice entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockPriceMapper {

    /**
     * Converts a StockPrice entity to a StockPriceResponse DTO.
     *
     * @param stockPrice The StockPrice entity.
     * @return The mapped StockPriceResponse.
     */
    @Mapping(source = "stock.stockId", target = "stockId")
    @Mapping(source = "stock.symbol", target = "symbol")
    @Mapping(source = "stock.name", target = "stockName")
    @Mapping(source = "stock.image", target = "image")
    @Mapping(source = "intervalClose", target = "price")
    @Mapping(source = "intervalOpen", target = "openPrice")
    @Mapping(source = "intervalVolume", target = "volume")
    @Mapping(target = "changePercent", ignore = true)
    StockPriceResponse toResponse(StockPrice stockPrice);

    /**
     * Converts a list of StockPrice entities to StockPriceResponse DTOs.
     *
     * @param stockPrices The list of StockPrice entities.
     * @return The list of mapped StockPriceResponse objects.
     */
    List<StockPriceResponse> toResponseList(List<StockPrice> stockPrices);

    /**
     * Converts a StockPrice entity to a PricePoint for history responses.
     *
     * @param stockPrice The StockPrice entity.
     * @return The mapped PricePoint.
     */
    @Mapping(source = "intervalClose", target = "price")
    @Mapping(source = "previousClose", target = "previousClose")
    @Mapping(source = "intervalOpen", target = "openPrice")
    @Mapping(source = "dayHigh", target = "dayHigh")
    @Mapping(source = "dayLow", target = "dayLow")
    @Mapping(source = "intervalVolume", target = "volume")
    @Mapping(source = "intervalOpen", target = "intervalOpen")
    @Mapping(source = "intervalHigh", target = "intervalHigh")
    @Mapping(source = "intervalLow", target = "intervalLow")
    @Mapping(source = "intervalClose", target = "intervalClose")
    @Mapping(source = "intervalVolume", target = "intervalVolume")
    @Mapping(source = "priceTime", target = "priceTime")
    StockPriceHistoryResponse.PricePoint toPricePoint(StockPrice stockPrice);

    /**
     * Converts a list of StockPrice entities to PricePoint objects.
     *
     * @param stockPrices The list of StockPrice entities.
     * @return The list of mapped PricePoint objects.
     */
    List<StockPriceHistoryResponse.PricePoint> toPricePointList(List<StockPrice> stockPrices);
}
