package org.example.userservice.service;

import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.entity.User;
import org.example.userservice.exception.EmailInUseException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.exception.UsernameExistsException;
import org.example.userservice.mapper.RegisterRequestMapper;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.impl.UserServiceImpl;
import org.example.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterRequestMapper registerRequestMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEmail("admin@test.com");
        user.setPassword("encodedPassword");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("admin");
        registerRequest.setEmail("admin@test.com");
        registerRequest.setPassword("12345");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("12345");
    }

    @Test
    void findByUsername_shouldReturnUser() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        Optional<User> result =
                userService.findByUsername("admin");

        assertTrue(result.isPresent());

        verify(userRepository)
                .findByUsername("admin");
    }

    @Test
    void findByUsername_shouldThrowException() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.findByUsername("admin")
        );
    }

    @Test
    void login_shouldReturnToken() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "12345",
                "encodedPassword"))
                .thenReturn(true);

        when(jwtUtil.generateToken("admin"))
                .thenReturn("jwt-token");

        LoginResponse response =
                userService.login(loginRequest);

        assertEquals(
                "jwt-token",
                response.getToken()
        );

        assertEquals(1L, response.getUserId());

        assertEquals(
                "admin",
                response.getUsername()
        );
    }

    @Test
    void login_shouldThrowWhenUserNotFound() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.login(loginRequest)
        );
    }

    @Test
    void login_shouldThrowBadCredentials() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                anyString(),
                anyString()))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> userService.login(loginRequest)
        );
    }

    @Test
    void save_shouldThrowWhenUsernameExists() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        assertThrows(
                UsernameExistsException.class,
                () -> userService.save(registerRequest)
        );

        verify(userRepository, never())
                .save(any());
    }

    @Test
    void save_shouldThrowWhenEmailExists() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                EmailInUseException.class,
                () -> userService.save(registerRequest)
        );

        verify(userRepository, never())
                .save(any());
    }

    @Test
    void save_shouldSaveUser() {

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("12345"))
                .thenReturn("encodedPassword");

        when(registerRequestMapper.toEntity(registerRequest))
                .thenReturn(user);

        userService.save(registerRequest);

        verify(userRepository).save(user);

        assertEquals(
                "encodedPassword",
                registerRequest.getPassword()
        );
    }
}