package org.example.securitycommon.principal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtPrincipal {
    private Long userId;
    private String username;
}
