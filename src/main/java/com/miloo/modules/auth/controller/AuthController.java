package com.miloo.modules.auth.controller;

import com.miloo.common.exception.ApiException;
import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.auth.constant.AuthConstants;
import com.miloo.modules.auth.dto.*;
import com.miloo.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @GetMapping("/activate")
    public ResponseEntity<?> activate(
            @RequestParam("token") String token,
            @RequestHeader(value = "Accept", required = false) String acceptHeader
    ) {
        boolean isHtml = acceptHeader != null && acceptHeader.contains("text/html");

        try {
            AccountDto account = authService.activateAccount(token);

            if (isHtml) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(AuthConstants.ACTIVATION_SUCCESS_HTML);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account successfully activated! You can now log in.",
                    "account", account
            ));
        } catch (ApiException ex) {
            if (isHtml) {
                String errorHtml = AuthConstants.buildActivationErrorHtml(ex.getMessage());
                return ResponseEntity.status(ex.getStatus())
                        .contentType(MediaType.TEXT_HTML)
                        .body(errorHtml);
            }
            throw ex;
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activatePost(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("token") : null;
        AccountDto account = authService.activateAccount(token);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Account successfully activated! You can now log in.",
                "account", account
        ));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
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
