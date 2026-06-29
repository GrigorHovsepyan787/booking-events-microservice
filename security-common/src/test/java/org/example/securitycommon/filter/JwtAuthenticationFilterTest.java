package org.example.securitycommon.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.securitycommon.parser.JwtParser;
import org.example.securitycommon.principal.JwtPrincipal;
import org.example.securitycommon.util.SecurityConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtParser jwtParser;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_NoAuthHeader_PassesChainWithoutAuthentication() throws ServletException, IOException {
        when(request.getHeader(SecurityConstants.AUTH_HEADER)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidPrefix_PassesChainWithoutAuthentication() throws ServletException, IOException {
        when(request.getHeader(SecurityConstants.AUTH_HEADER)).thenReturn("Basic plainTextPassword");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ParserThrowsException_SendsUnauthorizedError() throws ServletException, IOException {
        String rawToken = SecurityConstants.TOKEN_PREFIX + "corruptedToken";
        when(request.getHeader(SecurityConstants.AUTH_HEADER)).thenReturn(rawToken);
        when(jwtParser.getUsernameFromToken("corruptedToken")).thenThrow(new RuntimeException("Malformed JWT"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ValidToken_SetsAuthenticationInContext() throws ServletException, IOException {
        String cleanToken = "validToken123";
        String rawToken = SecurityConstants.TOKEN_PREFIX + cleanToken;
        String username = "john_doe";

        when(request.getHeader(SecurityConstants.AUTH_HEADER)).thenReturn(rawToken);
        when(jwtParser.getUsernameFromToken(cleanToken)).thenReturn(username);
        when(jwtParser.validateToken(cleanToken, username)).thenReturn(true);

        doReturn(List.of("ROLE_USER", "ROLE_ADMIN"))
                .doReturn(100L)
                .when(jwtParser).extractClaim(eq(cleanToken), any());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        assertEquals(100L, principal.getUserId());
        assertEquals(username, principal.getUsername());

        var authorities = authentication.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .toList();
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("ROLE_ADMIN"));
    }
}