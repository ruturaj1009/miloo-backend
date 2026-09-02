package com.miloo.modules.auth.service;

import com.miloo.common.exception.ApiException;
import com.miloo.config.JwtTokenProvider;
import com.miloo.modules.auth.dto.*;
import com.miloo.modules.auth.entity.AccountEntity;
import com.miloo.modules.auth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getPhoneNumber()) && !StringUtils.hasText(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Either phone number or email must be provided");
        }

        if (StringUtils.hasText(request.getPhoneNumber()) && accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number is already registered");
        }

        if (StringUtils.hasText(request.getEmail()) && accountRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        String passwordHash = StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword())
                : null;

        AccountEntity account = AccountEntity.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .isVerified(true)
                .build();

        AccountEntity savedAccount = accountRepository.save(account);

        String token = jwtTokenProvider.generateToken(savedAccount.getAccountId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedAccount.getAccountId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .account(toDto(savedAccount))
                .build();
    }

    public void login(LoginRequest request) {
        String destination = StringUtils.hasText(request.getPhoneNumber())
                ? request.getPhoneNumber()
                : request.getEmail();

        if (!StringUtils.hasText(destination)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number or email is required");
        }

        // Trigger OTP delivery
        otpService.generateOtp(destination);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String destination = StringUtils.hasText(request.getPhoneNumber())
                ? request.getPhoneNumber()
                : request.getEmail();

        if (!StringUtils.hasText(destination)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number or email is required");
        }

        boolean isValid = otpService.verifyOtp(destination, request.getOtp());
        if (!isValid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP code");
        }

        // Find existing account or create on-the-fly (seamless onboarding)
        Optional<AccountEntity> accountOpt = StringUtils.hasText(request.getPhoneNumber())
                ? accountRepository.findByPhoneNumber(request.getPhoneNumber())
                : accountRepository.findByEmail(request.getEmail());

        AccountEntity account = accountOpt.orElseGet(() -> {
            AccountEntity newAccount = AccountEntity.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .email(request.getEmail())
                    .isVerified(true)
                    .build();
            return accountRepository.save(newAccount);
        });

        if (!Boolean.TRUE.equals(account.getIsVerified())) {
            account.setIsVerified(true);
            account = accountRepository.save(account);
        }

        String token = jwtTokenProvider.generateToken(account.getAccountId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(account.getAccountId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .account(toDto(account))
                .build();
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountById(UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        return toDto(account);
    }

    public AccountDto toDto(AccountEntity entity) {
        return AccountDto.builder()
                .accountId(entity.getAccountId())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .isVerified(entity.getIsVerified())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}
