package org.example.apigateway.filter;

import org.example.securitycommon.parser.JwtParser;
import org.example.securitycommon.principal.JwtPrincipal;
import org.example.securitycommon.util.SecurityConstants;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtReactiveFilter implements WebFilter {

    private final JwtParser jwtParser;

    public JwtReactiveFilter(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTH_HEADER);
        String username = null;
        String authToken = null;

        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            authToken = authHeader.substring(7);
            try {
                username = jwtParser.getUsernameFromToken(authToken);
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        if (username != null) {
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

                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            }
        }

        return chain.filter(exchange);
    }
}