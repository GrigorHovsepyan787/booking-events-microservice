package org.example.securitycommon.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.securitycommon.parser.JwtParser;
import org.example.securitycommon.principal.JwtPrincipal;
import org.example.securitycommon.util.SecurityConstants;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtParser jwtParser;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    @NonNull HttpServletResponse httpServletResponse,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestHeader = httpServletRequest.getHeader(SecurityConstants.AUTH_HEADER);
        String username = null;
        String authToken = null;

        if (requestHeader != null && requestHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            authToken = requestHeader.substring(7);
            try {
                username = jwtParser.getUsernameFromToken(authToken);
            } catch (Exception e) {
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtParser.validateToken(authToken, username)) {

                List<String> roles = jwtParser.extractClaim(authToken, claims -> claims.get("roles", List.class));
                List<SimpleGrantedAuthority> authorities = roles != null ?
                        roles.stream().map(SimpleGrantedAuthority::new).toList() :
                        List.of();

                Number userIdClaim = jwtParser.extractClaim(authToken, claims -> claims.get("userId", Number.class));
                Long userId = userIdClaim != null ? userIdClaim.longValue() : null;

                JwtPrincipal principal = new JwtPrincipal(userId, username);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}