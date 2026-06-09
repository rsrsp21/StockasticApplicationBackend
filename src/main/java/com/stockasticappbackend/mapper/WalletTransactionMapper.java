package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.stockasticappbackend.dto.wallet.WalletTransactionResponse;
import com.stockasticappbackend.model.entity.WalletTransaction;

/**
 * MapStruct mapper for WalletTransaction entity operations.
 */
@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {

    /**
     * Converts a WalletTransaction entity to a WalletTransactionResponse DTO.
     *
     * @param transaction The WalletTransaction entity.
     * @return The mapped WalletTransactionResponse.
     */
    WalletTransactionResponse toResponse(WalletTransaction transaction);

    /**
     * Converts a list of WalletTransaction entities to a list of DTOs.
     *
     * @param transactions The list of WalletTransaction entities.
     * @return The list of mapped WalletTransactionResponse objects.
     */
    List<WalletTransactionResponse> toResponseList(List<WalletTransaction> transactions);
}
