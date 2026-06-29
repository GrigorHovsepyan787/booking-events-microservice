package org.example.userservice.service.security;

import org.example.userservice.entity.User;
import org.example.userservice.entity.UserType;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private final String username = "testUser";
    private final String password = "encodedPassword123";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(username);
        testUser.setPassword(password);
        testUser.setUserType(UserType.ACTIVE);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo(password);
        assertTrue(userDetails.isEnabled());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void loadUserByUsername_ShouldReturnEnabledUser_WhenUserTypeIsActive() {
        testUser.setUserType(UserType.ACTIVE);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull();
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_ShouldReturnEnabledUser_WhenUserTypeIsAdmin() {
        testUser.setUserType(UserType.ADMIN);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull();
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_ShouldReturnDisabledUser_WhenUserTypeIsUnenabled() {
        testUser.setUserType(UserType.UNENABLED);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull();
        assertFalse(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenUsernameIsNull() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByUsername(null);
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenUsernameIsEmpty() {
        String emptyUsername = "";
        when(userRepository.findByUsername(emptyUsername)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(emptyUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByUsername(emptyUsername);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserWithCorrectAuthorities_WhenUserExists() {
        testUser.setUserType(UserType.ADMIN);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ADMIN");
    }

    @Test
    void loadUserByUsername_ShouldReturnUserWithActiveAuthority_WhenUserTypeIsActive() {
        testUser.setUserType(UserType.ACTIVE);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ACTIVE");
    }
}