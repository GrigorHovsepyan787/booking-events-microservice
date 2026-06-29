package org.example.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exception.InvalidRefreshTokenException;
import org.example.userservice.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Value("${jwt.refresh-expiration}")
    private Duration expire;
    private static final String PREFIX = "refresh_token:";
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String create(Long userId) {
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + refreshToken,
                userId.toString(),
                expire);
        return refreshToken;
    }

    @Override
    public Long validate(String token) {
        String userId = redisTemplate.opsForValue().get(PREFIX + token);
        if (userId == null) {
            throw new InvalidRefreshTokenException("Refresh token is invalid or expired");
        }
        return Long.valueOf(userId);
    }

    @Override
    public String rotate(Long userId, String oldToken) {
        logout(oldToken);
        return create(userId);
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
