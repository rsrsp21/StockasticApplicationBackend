package com.stockasticappbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stockasticappbackend.dto.wallet.WalletResponse;
import com.stockasticappbackend.model.entity.Wallet;

/**
 * MapStruct mapper for Wallet entity operations.
 */
@Mapper(componentModel = "spring")
public interface WalletMapper {

    /**
     * Converts a Wallet entity to a WalletResponse DTO.
     *
     * @param wallet The Wallet entity.
     * @return The mapped WalletResponse.
     */
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "totalBalance", expression = "java(wallet.getTotalBalance())")
    WalletResponse toResponse(Wallet wallet);
}
