package org.example.userservice.service;

public interface RefreshTokenService {
    String create(Long userId);

    Long validate(String token);

    String rotate(Long userId, String oldToken);

    void logout(String token);
}
