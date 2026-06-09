package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stockasticappbackend.dto.user.AdminUserResponse;
import com.stockasticappbackend.model.entity.AppUser;

/**
 * MapStruct mapper for user administrative operations.
 * Converts AppUser entities to AdminUserResponse DTOs.
 */
@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    /**
     * Converts an AppUser entity to an AdminUserResponse DTO.
     *
     * @param user The AppUser entity.
     * @return The mapped AdminUserResponse.
     */
    @Mapping(source = "userStatus", target = "status")
    @Mapping(source = "role", target = "role")
    AdminUserResponse toResponse(AppUser user);

    /**
     * Converts a list of AppUser entities to AdminUserResponse DTOs.
     *
     * @param users The list of AppUser entities.
     * @return The list of mapped AdminUserResponse objects.
     */
    List<AdminUserResponse> toResponseList(List<AppUser> users);
}