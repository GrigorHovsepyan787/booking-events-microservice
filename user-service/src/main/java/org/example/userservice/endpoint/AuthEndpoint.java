package org.example.userservice.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.LoginResponse;
import org.example.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthEndpoint {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.save(request);
        return ResponseEntity.ok().build();
    }
}
