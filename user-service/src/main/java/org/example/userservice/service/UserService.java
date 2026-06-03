package org.example.userservice.service;

import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.entity.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);

    LoginResponse login(LoginRequest loginRequest);

    Optional<User> findByEmail(String email);

    void save(RegisterRequest request);
}
