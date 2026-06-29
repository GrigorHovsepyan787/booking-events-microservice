package org.example.userservice.repository;

import org.example.userservice.entity.User;
import org.example.userservice.entity.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    private User createUser() {
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setUserType(UserType.ACTIVE);
        return user;
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenUserExists() {

        User user = createUser();
        userRepository.save(user);

        Optional<User> result = userRepository.findByUsername("testUser");

        assertTrue(result.isPresent());
        assertEquals("testUser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenUserDoesNotExist() {

        Optional<User> result = userRepository.findByUsername("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenUserExists() {

        User user = createUser();
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        assertEquals("testUser", result.get().getUsername());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenUserDoesNotExist() {

        Optional<User> result = userRepository.findByEmail("unknown@example.com");

        assertTrue(result.isEmpty());
    }
}