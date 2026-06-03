package org.example.userservice.endpoint;


import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthEndpointTests {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthEndpoint authEndpoint;

    @Test
    void login_shouldReturnToken() {

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("12345");

        LoginResponse response = new LoginResponse();
        response.setToken("jwt-token");

        when(userService.login(request))
                .thenReturn(response);

        ResponseEntity<LoginResponse> result =
                authEndpoint.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert result.getBody() != null;
        assertEquals("jwt-token", result.getBody().getToken());
    }

    @Test
    void register_shouldReturnOk() {

        RegisterRequest request = new RegisterRequest();

        ResponseEntity<Void> result =
                authEndpoint.register(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(userService).save(request);
    }
}