package org.example.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.kafka.event.UserCreatedEvent;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RefreshRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.entity.User;
import org.example.userservice.exception.EmailInUseException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.exception.UsernameExistsException;
import org.example.userservice.kafka.producer.UserEventProducer;
import org.example.userservice.mapper.RegisterRequestMapper;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.RefreshTokenService;
import org.example.userservice.service.UserService;
import org.example.userservice.util.JwtGenerator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RegisterRequestMapper registerRequestMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtGenerator jwtGenerator;
    private final UserDetailsService userDetailsService;
    private final UserEventProducer producer;
    private final RefreshTokenService refreshTokenService;

    @Override
    public Optional<User> findByUsername(String username) {
        log.info("Attempting to find user by username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            log.warn("User with username: {} not found", username);
            throw new UserNotFoundException("Username not found");
        }
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.info("Attempting to find user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Processing login request for username: {}", loginRequest.getUsername());
        User user = fetchUserByUsername(loginRequest.getUsername());
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return generateToken(user, userDetails);
        }
        log.warn("Login failed: Bad credentials for username {}", loginRequest.getUsername());
        throw new BadCredentialsException("Invalid username or password");
    }

    @Override
    @Transactional
    public void save(RegisterRequest request) {
        log.info("Processing registration request for username: {}, email: {}", request.getUsername(), request.getEmail());
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username {} already exists", request.getUsername());
            throw new UsernameExistsException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed: Email {} is already in use", request.getEmail());
            throw new EmailInUseException("Email already exists");
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        User user = registerRequestMapper.toEntity(request);
        userRepository.save(user);
        log.info("User {} successfully saved to database with ID: {}", user.getUsername(), user.getId());
        producer.sendUserCreated(UserCreatedEvent.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail()).build());
        log.info("UserCreatedEvent sent for user ID: {}", user.getId());
    }

    @Override
    public void logout(RefreshRequest request) {
        log.info("Processing logout request");
        refreshTokenService.logout(request.getRefreshToken());
        log.info("Refresh token successfully invalidated/logged out");
    }

    @Override
    public LoginResponse refresh(RefreshRequest refreshRequest) {
        log.info("Processing token refresh request");
        Long userId = refreshTokenService.validate(refreshRequest.getRefreshToken());
        User user = fetchUser(userId);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        log.info("Refreshing tokens for userId={}", userId);
        return LoginResponse.builder()
                .accessToken(jwtGenerator.generateToken(userDetails))
                .refreshToken(refreshTokenService.rotate(userId, refreshRequest.getRefreshToken()))
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    private LoginResponse generateToken(User user, UserDetails userDetails) {
        log.info("Password matched for user: {}. Generating tokens.", user.getUsername());
        return LoginResponse.builder()
                .accessToken(jwtGenerator.generateToken(userDetails))
                .refreshToken(refreshTokenService.create(user.getId()))
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    private User fetchUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> {
            log.error("Fetch failed: User with ID {} not found", userId);
            return new UserNotFoundException("User not found");
        });
    }

    private User fetchUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Fetch failed: User with username {} not found", username);
                    return new UserNotFoundException("User not found");
                });
    }
}
