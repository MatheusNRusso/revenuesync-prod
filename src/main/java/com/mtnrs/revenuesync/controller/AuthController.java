package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.dto.auth.AuthResponse;
import com.mtnrs.revenuesync.dto.auth.LoginRequest;
import com.mtnrs.revenuesync.dto.auth.RegisterRequest;
import com.mtnrs.revenuesync.infra.security.AuthProperties;
import com.mtnrs.revenuesync.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        if (!authProperties.isAllowDirectRegistration()) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of(
                            "error", "Direct registration is disabled",
                            "message", "This platform is GitHub-first. Please sign in with GitHub."
                    ));
        }
        var token = authService.registerUser(request.name(), request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        var token = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validate() {
        return ResponseEntity.ok().build();
    }
}