package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.stockasticappbackend.dto.wallet.BankAccountResponse;
import com.stockasticappbackend.dto.wallet.LinkBankAccountRequest;
import com.stockasticappbackend.model.entity.BankAccount;
import com.stockasticappbackend.util.AccountNumberMasker;

/**
 * MapStruct mapper for BankAccount entity operations.
 */
@Mapper(componentModel = "spring")
public interface BankAccountMapper {

    /**
     * Converts a BankAccount entity to a BankAccountResponse DTO.
     * Account number is masked for security.
     *
     * @param bankAccount The BankAccount entity.
     * @return The mapped BankAccountResponse.
     */
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "maskedAccountNumber", source = "accountNumber", qualifiedByName = "maskAccountNumber")
    BankAccountResponse toResponse(BankAccount bankAccount);

    /**
     * Converts a list of BankAccount entities to a list of DTOs.
     *
     * @param bankAccounts The list of BankAccount entities.
     * @return The list of mapped BankAccountResponse objects.
     */
    List<BankAccountResponse> toResponseList(List<BankAccount> bankAccounts);

    /**
     * Converts a LinkBankAccountRequest DTO to a BankAccount entity.
     *
     * @param request The LinkBankAccountRequest DTO.
     * @return The mapped BankAccount entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "isPrimary", ignore = true)
    BankAccount toEntity(LinkBankAccountRequest request);

    /**
     * Masks the account number for security.
     *
     * @param accountNumber The full account number.
     * @return The masked account number.
     */
    @Named("maskAccountNumber")
    default String maskAccountNumber(String accountNumber) {
        return AccountNumberMasker.mask(accountNumber);
    }
}
