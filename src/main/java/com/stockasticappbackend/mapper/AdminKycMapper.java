package com.stockasticappbackend.mapper;

import com.stockasticappbackend.dto.user.PendingKycResponse;
import com.stockasticappbackend.model.entity.Kyc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for KYC administrative operations.
 * Converts Kyc entities to PendingKycResponse DTOs for admin review.
 */
@Mapper(componentModel = "spring")
public interface AdminKycMapper {

    /**
     * Converts a Kyc entity to a PendingKycResponse DTO.
     *
     * @param kyc The Kyc entity.
     * @return The mapped PendingKycResponse.
     */
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.name", target = "name")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.profileImagePath", target = "profileImagePath")
    @Mapping(source = "panNumber", target = "panNumber")
    @Mapping(source = "aadhaarNumber", target = "aadhaarNumber")
    @Mapping(source = "attemptCount", target = "attemptCount")
    @Mapping(source = "submittedAt", target = "submittedAt")
    PendingKycResponse toResponse(Kyc kyc);

    /**
     * Converts a list of Kyc entities to PendingKycResponse DTOs.
     *
     * @param kycs The list of Kyc entities.
     * @return The list of mapped PendingKycResponse objects.
     */
    List<PendingKycResponse> toResponseList(List<Kyc> kycs);
}
