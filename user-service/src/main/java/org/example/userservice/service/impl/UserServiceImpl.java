package org.example.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.common.kafka.event.UserCreatedEvent;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.entity.User;
import org.example.userservice.exception.EmailInUseException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.exception.UsernameExistsException;
import org.example.userservice.kafka.producer.UserEventProducer;
import org.example.userservice.mapper.RegisterRequestMapper;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.UserService;
import org.example.userservice.util.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RegisterRequestMapper registerRequestMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil tokenUtil;
    private final UserDetailsService userDetailsService;
    private final UserEventProducer producer;

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new UserNotFoundException("Username not found");
        }
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return LoginResponse.builder()
                    .token(tokenUtil.generateToken(userDetails))
                    .username(user.getUsername())
                    .userId(user.getId())
                    .build();
        }
        throw new BadCredentialsException("Invalid username or password");
    }

    @Override
    @Transactional
    public void save(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UsernameExistsException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailInUseException("Email already exists");
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        User user = registerRequestMapper.toEntity(request);
        userRepository.save(user);
        producer.sendUserCreated(UserCreatedEvent.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail()).build());
    }
}
