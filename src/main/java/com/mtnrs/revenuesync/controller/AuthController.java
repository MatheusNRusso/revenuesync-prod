package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.dto.auth.AuthResponse;
import com.mtnrs.revenuesync.dto.auth.LoginRequest;
import com.mtnrs.revenuesync.dto.auth.RegisterRequest;
import com.mtnrs.revenuesync.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        authService.registerUser(request.name(), request.email(), request.password());
        return ResponseEntity.ok(Map.of(
                "message", "Registration successful. Please check your email for the verification code."
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @RequestBody Map<String, String> body
    ) {
        String token = authService.verifyEmail(body.get("email"), body.get("token"));
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request
    ) {
        var token = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validate() {
        return ResponseEntity.ok().build();
    }
}
