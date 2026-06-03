package org.example.userservice.mapper;

import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginRequestMapper {
    User toEntity(LoginRequest loginRequest);
}
