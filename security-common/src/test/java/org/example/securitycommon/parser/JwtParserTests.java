package org.example.securitycommon.parser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtParserTests {

    private JwtParser jwtParser;

    private String validToken;
    private final String expectedUsername = "testUser";

    @BeforeEach
    void setUp() {
        jwtParser = new JwtParser();
        String base64Secret = "YTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
        ReflectionTestUtils.setField(jwtParser, "SECRET_KEY", base64Secret);

        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        validToken = Jwts.builder()
                .setSubject(expectedUsername)
                .claim("roles", List.of("ROLE_USER"))
                .claim("userId", 42L)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void getUsernameFromToken_ValidToken_ReturnsUsername() {
        String username = jwtParser.getUsernameFromToken(validToken);
        assertEquals(expectedUsername, username);
    }

    @Test
    void validateToken_CorrectUsernameAndNotExpired_ReturnsTrue() {
        Boolean isValid = jwtParser.validateToken(validToken, expectedUsername);
        assertTrue(isValid);
    }

    @Test
    void validateToken_WrongUsername_ReturnsFalse() {
        Boolean isValid = jwtParser.validateToken(validToken, "wrongUser");
        falseOrExceptionCheck(isValid);
    }

    @Test
    void extractClaim_CustomClaims_ReturnsCorrectValues() {
        List<?> roles = jwtParser.extractClaim(validToken, claims -> claims.get("roles", List.class));
        Number userId = jwtParser.extractClaim(validToken, claims -> claims.get("userId", Number.class));

        assertNotNull(roles);
        assertEquals("ROLE_USER", roles.getFirst());
        assertEquals(42, userId.intValue());
    }

    @Test
    void validateToken_ExpiredOrInvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.structure";
        Boolean isValid = jwtParser.validateToken(invalidToken, expectedUsername);
        assertFalse(isValid);
    }

    private void falseOrExceptionCheck(Boolean condition) {
        assertFalse(condition);
    }
}