package org.example.userservice.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTests {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            Base64.getEncoder()
                    .encodeToString("mySuperSecretKeyForJwtTokenGeneration123456"
                            .getBytes());

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                jwtUtil,
                "SECRET_KEY",
                SECRET
        );

        ReflectionTestUtils.setField(
                jwtUtil,
                "EXPIRATION_TIME",
                60_000L // 1 минута
        );
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtUtil.generateToken("admin");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getUsernameFromToken_shouldReturnUsername() {
        String token = jwtUtil.generateToken("admin");

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals("admin", username);
    }

    @Test
    void extractUsername_shouldReturnUsername() {
        String token = jwtUtil.generateToken("testUser");

        String username = jwtUtil.extractUsername(token);

        assertEquals("testUser", username);
    }

    @Test
    void extractExpiration_shouldReturnExpirationDate() {
        String token = jwtUtil.generateToken("admin");

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("admin");

        Boolean result = jwtUtil.validateToken(
                token,
                "admin"
        );

        assertTrue(result);
    }

    @Test
    void validateToken_shouldReturnFalseForWrongUsername() {
        String token = jwtUtil.generateToken("admin");

        Boolean result = jwtUtil.validateToken(
                token,
                "user"
        );

        assertFalse(result);
    }

    @Test
    void getClaimFromToken_shouldExtractSubject() {
        String token = jwtUtil.generateToken("admin");

        String subject = jwtUtil.getClaimFromToken(
                token,
                Claims::getSubject
        );

        assertEquals("admin", subject);
    }

    @Test
    void extractClaim_shouldExtractExpiration() {
        String token = jwtUtil.generateToken("admin");

        Date expiration = jwtUtil.extractClaim(
                token,
                Claims::getExpiration
        );

        assertNotNull(expiration);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken()
            throws InterruptedException {

        ReflectionTestUtils.setField(
                jwtUtil,
                "EXPIRATION_TIME",
                1L
        );

        String token = jwtUtil.generateToken("admin");

        Thread.sleep(20);

        Boolean result = jwtUtil.validateToken(
                token,
                "admin"
        );

        assertFalse(result);
    }

    @Test
    void extractUsername_shouldThrowExceptionForInvalidToken() {
        assertThrows(
                Exception.class,
                () -> jwtUtil.extractUsername("invalid-token")
        );
    }
}