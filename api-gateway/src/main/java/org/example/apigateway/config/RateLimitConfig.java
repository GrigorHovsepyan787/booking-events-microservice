package org.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimitConfig {
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange
                                .getRequest()
                                .getRemoteAddress())
                        .getAddress()
                        .getHostAddress()
        );
    }

    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10);
    }
}
