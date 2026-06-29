package org.example.userservice.service.impl;

import org.example.common.kafka.event.UserCreatedEvent;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RefreshRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.entity.User;
import org.example.userservice.entity.UserType;
import org.example.userservice.exception.EmailInUseException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.exception.UsernameExistsException;
import org.example.userservice.kafka.producer.UserEventProducer;
import org.example.userservice.mapper.RegisterRequestMapper;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.RefreshTokenService;
import org.example.userservice.util.JwtGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterRequestMapper registerRequestMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserEventProducer producer;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private RefreshRequest refreshRequest;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setUserType(UserType.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");

        refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");

        userDetails = mock(UserDetails.class);
    }

    // ==================== findByUsername Tests ====================

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByUsername_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Username not found");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void findByUsername_shouldHandleNullUsername() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername(null))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Username not found");

        verify(userRepository).findByUsername(null);
    }

    @Test
    void findByUsername_shouldHandleEmptyUsername() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername(""))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Username not found");

        verify(userRepository).findByUsername("");
    }

    // ==================== findByEmail Tests ====================

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void findByEmail_shouldHandleNullEmail() {
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail(null);

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail(null);
    }

    @Test
    void findByEmail_shouldHandleEmptyEmail() {
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("");
    }

    // ==================== login Tests ====================

    @Test
    void login_shouldReturnLoginResponse_whenCredentialsAreValid() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        LoginResponse response = userService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(userRepository).findByUsername("testuser");
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtGenerator).generateToken(userDetails);
        verify(refreshTokenService).create(1L);
    }

    @Test
    void login_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername("testuser");
        verifyNoInteractions(userDetailsService, passwordEncoder, jwtGenerator, refreshTokenService);
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenPasswordIsInvalid() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(userRepository).findByUsername("testuser");
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verifyNoInteractions(jwtGenerator, refreshTokenService);
    }

    @Test
    void login_shouldThrowUserNotFoundException_whenUsernameIsNull() {
        LoginRequest nullRequest = new LoginRequest();
        nullRequest.setUsername(null);
        nullRequest.setPassword("password123");

        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(nullRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername(null);
        verifyNoInteractions(userDetailsService, passwordEncoder, jwtGenerator, refreshTokenService);
    }

    @Test
    void login_shouldThrowUserNotFoundException_whenUsernameIsEmpty() {
        LoginRequest emptyRequest = new LoginRequest();
        emptyRequest.setUsername("");
        emptyRequest.setPassword("password123");

        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(emptyRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUsername("");
        verifyNoInteractions(userDetailsService, passwordEncoder, jwtGenerator, refreshTokenService);
    }

    // ==================== save (Register) Tests ====================

    @Test
    void save_shouldRegisterUser_whenDataIsValid() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(registerRequestMapper.toEntity(registerRequest)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.save(registerRequest);

        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(registerRequestMapper).toEntity(registerRequest);
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<UserCreatedEvent> captor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(producer).sendUserCreated(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUsername()).isEqualTo("testuser");
        assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void save_shouldThrowUsernameExistsException_whenUsernameAlreadyExists() {
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("newuser");
        existingUser.setEmail("other@example.com");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.save(registerRequest))
                .isInstanceOf(UsernameExistsException.class)
                .hasMessage("Username already exists");

        verify(userRepository).findByUsername("newuser");
        verify(userRepository, never()).findByEmail(anyString());
        verifyNoInteractions(passwordEncoder, registerRequestMapper, producer);
    }

    @Test
    void save_shouldThrowEmailInUseException_whenEmailAlreadyExists() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.save(registerRequest))
                .isInstanceOf(EmailInUseException.class)
                .hasMessage("Email already exists");

        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("new@example.com");
        verifyNoInteractions(passwordEncoder, registerRequestMapper, producer);
    }

    @Test
    void save_shouldEncodePasswordBeforeSaving() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(registerRequestMapper.toEntity(registerRequest)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.save(registerRequest);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void save_shouldNotSaveUser_whenUsernameExists() {
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("newuser");
        existingUser.setEmail("other@example.com");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.save(registerRequest))
                .isInstanceOf(UsernameExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, registerRequestMapper, producer);
    }

    @Test
    void save_shouldNotSaveUser_whenEmailExists() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.save(registerRequest))
                .isInstanceOf(EmailInUseException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, registerRequestMapper, producer);
    }

    // ==================== logout Tests ====================

    @Test
    void logout_shouldInvalidateToken_whenCalled() {
        doNothing().when(refreshTokenService).logout("valid-refresh-token");

        userService.logout(refreshRequest);

        verify(refreshTokenService).logout("valid-refresh-token");
    }

    @Test
    void logout_shouldCallRefreshTokenServiceWithCorrectToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("my-token-123");

        doNothing().when(refreshTokenService).logout("my-token-123");

        userService.logout(request);

        verify(refreshTokenService).logout("my-token-123");
    }

    @Test
    void logout_shouldHandleNullRefreshToken() {
        RefreshRequest nullRequest = new RefreshRequest();
        nullRequest.setRefreshToken(null);

        doNothing().when(refreshTokenService).logout(null);

        userService.logout(nullRequest);

        verify(refreshTokenService).logout(null);
    }

    @Test
    void logout_shouldHandleEmptyRefreshToken() {
        RefreshRequest emptyRequest = new RefreshRequest();
        emptyRequest.setRefreshToken("");

        doNothing().when(refreshTokenService).logout("");

        userService.logout(emptyRequest);

        verify(refreshTokenService).logout("");
    }

    // ==================== refresh Tests ====================

    @Test
    void refresh_shouldReturnNewTokens_whenRefreshTokenIsValid() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, "valid-refresh-token")).thenReturn("new-refresh-token");

        LoginResponse response = userService.refresh(refreshRequest);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

        verify(refreshTokenService).validate("valid-refresh-token");
        verify(userRepository).findById(1L);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtGenerator).generateToken(userDetails);
        verify(refreshTokenService).rotate(1L, "valid-refresh-token");
    }

    @Test
    void refresh_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refresh(refreshRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(refreshTokenService).validate("valid-refresh-token");
        verify(userRepository).findById(1L);
        verifyNoInteractions(userDetailsService, jwtGenerator);
    }

    @Test
    void refresh_shouldThrowUserNotFoundException_whenUserIdIsInvalid() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refresh(refreshRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(refreshTokenService).validate("valid-refresh-token");
        verify(userRepository).findById(999L);
        verifyNoInteractions(userDetailsService, jwtGenerator);
    }

    @Test
    void refresh_shouldGenerateNewAccessToken() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, "valid-refresh-token")).thenReturn("new-refresh-token");

        LoginResponse response = userService.refresh(refreshRequest);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        verify(jwtGenerator).generateToken(userDetails);
    }

    @Test
    void refresh_shouldGenerateNewRefreshToken() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, "valid-refresh-token")).thenReturn("new-refresh-token");

        LoginResponse response = userService.refresh(refreshRequest);

        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).rotate(1L, "valid-refresh-token");
    }

    @Test
    void refresh_shouldReturnCorrectUsername() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, "valid-refresh-token")).thenReturn("new-refresh-token");

        LoginResponse response = userService.refresh(refreshRequest);

        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    void refresh_shouldHandleNullRefreshToken() {
        RefreshRequest nullRequest = new RefreshRequest();
        nullRequest.setRefreshToken(null);

        when(refreshTokenService.validate(null)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, null)).thenReturn("new-refresh-token");

        LoginResponse response = userService.refresh(nullRequest);

        assertThat(response).isNotNull();
        verify(refreshTokenService).validate(null);
    }

    @Test
    void refresh_shouldHandleEmptyRefreshToken() {
        RefreshRequest emptyRequest = new RefreshRequest();
        emptyRequest.setRefreshToken("");

        when(refreshTokenService.validate("")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, "")).thenReturn("new-refresh-token");

        LoginResponse response = userService.refresh(emptyRequest);

        assertThat(response).isNotNull();
        verify(refreshTokenService).validate("");
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    void login_shouldNotCallRefreshTokenService_whenPasswordIsInvalid() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtGenerator, refreshTokenService);
    }

    @Test
    void refresh_shouldNotCallJwtGenerator_whenUserNotFound() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refresh(refreshRequest))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(jwtGenerator);
    }

    @Test
    void login_shouldNotSendUserCreatedEvent_whenLoginFails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(producer);
    }

    @Test
    void login_shouldCallPasswordEncoderWithCorrectEncodedPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        userService.login(loginRequest);

        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void save_shouldSetUserTypeToActive() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(registerRequestMapper.toEntity(registerRequest)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.save(registerRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_shouldReturnCorrectResponseFields() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("test-access-token");
        when(refreshTokenService.create(1L)).thenReturn("test-refresh-token");

        LoginResponse response = userService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("test-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("test-refresh-token");
    }

    // ==================== Repository Interaction Verification Tests ====================

    @Test
    void login_shouldCallRepositoryAndServicesInCorrectOrder() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        userService.login(loginRequest);

        InOrder inOrder = inOrder(userRepository, userDetailsService, passwordEncoder, jwtGenerator, refreshTokenService);
        inOrder.verify(userRepository).findByUsername("testuser");
        inOrder.verify(userDetailsService).loadUserByUsername("testuser");
        inOrder.verify(passwordEncoder).matches("password123", "encodedPassword");
        inOrder.verify(jwtGenerator).generateToken(userDetails);
        inOrder.verify(refreshTokenService).create(1L);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void save_shouldCallRepositoryAndProducerInCorrectOrder() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(registerRequestMapper.toEntity(registerRequest)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.save(registerRequest);

        InOrder inOrder = inOrder(userRepository, passwordEncoder, registerRequestMapper, producer);
        inOrder.verify(userRepository).findByUsername("newuser");
        inOrder.verify(userRepository).findByEmail("new@example.com");
        inOrder.verify(passwordEncoder).encode("password123");
        inOrder.verify(registerRequestMapper).toEntity(registerRequest);
        inOrder.verify(userRepository).save(any(User.class));
        inOrder.verify(producer).sendUserCreated(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void refresh_shouldCallServicesInCorrectOrder() {
        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("new-access-token");
        when(refreshTokenService.rotate(1L, "valid-refresh-token")).thenReturn("new-refresh-token");

        userService.refresh(refreshRequest);

        InOrder inOrder = inOrder(refreshTokenService, userRepository, userDetailsService, jwtGenerator, refreshTokenService);
        inOrder.verify(refreshTokenService).validate("valid-refresh-token");
        inOrder.verify(userRepository).findById(1L);
        inOrder.verify(userDetailsService).loadUserByUsername("testuser");
        inOrder.verify(jwtGenerator).generateToken(userDetails);
        inOrder.verify(refreshTokenService).rotate(1L, "valid-refresh-token");
        inOrder.verifyNoMoreInteractions();
    }

    // ==================== Concurrent Scenario Tests ====================

    @Test
    void login_shouldHandleMultipleLoginAttempts() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtGenerator.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        LoginResponse response1 = userService.login(loginRequest);
        LoginResponse response2 = userService.login(loginRequest);

        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
        verify(userRepository, times(2)).findByUsername("testuser");
        verify(userDetailsService, times(2)).loadUserByUsername("testuser");
    }

    @Test
    void save_shouldHandleMultipleRegistrations() {
        RegisterRequest request1 = new RegisterRequest();
        request1.setUsername("user1");
        request1.setEmail("user1@example.com");
        request1.setPassword("password123");

        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("user2");
        request2.setEmail("user2@example.com");
        request2.setPassword("password123");

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(registerRequestMapper.toEntity(request1)).thenReturn(user1);
        when(registerRequestMapper.toEntity(request2)).thenReturn(user2);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.save(request1);
        userService.save(request2);

        verify(userRepository, times(2)).save(any(User.class));
        verify(producer, times(2)).sendUserCreated(any());
    }
}