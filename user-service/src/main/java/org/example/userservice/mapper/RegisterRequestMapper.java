package org.example.userservice.mapper;

import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegisterRequestMapper {
    @Mapping(target = "userType", constant = "ACTIVE")
    User toEntity(RegisterRequest request);
}
