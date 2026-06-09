package com.stockasticappbackend.mapper;

import com.stockasticappbackend.dto.user.CreateUserRequest;
import com.stockasticappbackend.dto.user.UserResponse;
import com.stockasticappbackend.model.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for user operations.
 * Handles conversions between AppUser entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    /**
     * Converts a CreateUserRequest DTO to an AppUser entity.
     *
     * @param request The CreateUserRequest DTO.
     * @return The mapped AppUser entity.
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "profileImagePath", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AppUser toEntity(CreateUserRequest request);

    /**
     * Converts an AppUser entity to a UserResponse DTO.
     *
     * @param user The AppUser entity.
     * @return The mapped UserResponse.
     */
    UserResponse toResponse(AppUser user);
}
