package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.model.entity.Order;

/**
 * MapStruct mapper for Order operations.
 * Handles conversions between Order entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    /**
     * Converts an Order entity to an OrderResponse DTO.
     *
     * @param order The Order entity.
     * @return The mapped OrderResponse.
     */
    @Mapping(target = "symbol", source = "stock.symbol")
    @Mapping(target = "stockName", source = "stock.name")
    OrderResponse toResponse(Order order);

    /**
     * Converts a list of Order entities to OrderResponse DTOs.
     *
     * @param orders The list of Order entities.
     * @return The list of mapped OrderResponse objects.
     */
    List<OrderResponse> toResponseList(List<Order> orders);
}
