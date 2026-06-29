package org.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {
    private final RedisRateLimiter authRateLimiter;
    private final KeyResolver ipKeyResolver;

    public RouteConfig(RedisRateLimiter authRateLimiter, KeyResolver ipKeyResolver) {
        this.authRateLimiter = authRateLimiter;
        this.ipKeyResolver = ipKeyResolver;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service",
                        r -> r.path("/api/auth/**")
                                .filters(f -> f.requestRateLimiter(config -> config
                                        .setRateLimiter(authRateLimiter)
                                        .setKeyResolver(ipKeyResolver)))
                                .uri("http://user-service:8081"))

                .route("notification-service",
                        r -> r.path("/api/notifications/**")
                                .uri("http://notification-service:8082"))

                .route("event-service",
                        r -> r.path("/api/events/**")
                                .uri("http://event-service:8083"))

                .route("booking-service",
                        r -> r.path("/api/bookings/**")
                                .uri("http://booking-service:8084"))
                .build();
    }
}