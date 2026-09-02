package com.miloo.modules.auth.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.auth.dto.*;
import com.miloo.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        authService.login(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AccountDto> getCurrentAccount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        AccountDto account = authService.getAccountById(currentUserId);
        return ResponseEntity.ok(account);
    }
}
