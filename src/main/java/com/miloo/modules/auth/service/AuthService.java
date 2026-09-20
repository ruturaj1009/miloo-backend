package com.miloo.modules.auth.service;

import com.miloo.common.exception.ApiException;
import com.miloo.config.AuthProperties;
import com.miloo.config.JwtTokenProvider;
import com.miloo.modules.auth.dto.*;
import com.miloo.modules.auth.entity.AccountActivationEntity;
import com.miloo.modules.auth.entity.AccountEntity;
import com.miloo.modules.auth.repository.AccountActivationRepository;
import com.miloo.modules.auth.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final AccountActivationRepository accountActivationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final AuthEmailService authEmailService;
    private final AuthProperties authProperties;

    /**
     * Resolves international E.164 phone number formatting given phone number and optional country code.
     */
    public String resolveDestinationPhone(String phoneNumber, String countryCode) {
        if (!StringUtils.hasText(phoneNumber)) {
            return null;
        }
        String trimmedPhone = phoneNumber.trim();
        if (trimmedPhone.startsWith("+")) {
            return trimmedPhone;
        }
        if (StringUtils.hasText(countryCode)) {
            String cleanCode = countryCode.trim();
            if (!cleanCode.startsWith("+")) {
                cleanCode = "+" + cleanCode;
            }
            return cleanCode + trimmedPhone;
        }
        return trimmedPhone;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email address is required for registration and account activation");
        }

        String resolvedPhone = resolveDestinationPhone(request.getPhoneNumber(), request.getCountryCode());

        if (StringUtils.hasText(resolvedPhone) && accountRepository.existsByPhoneNumber(resolvedPhone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number is already registered");
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        String passwordHash = StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword())
                : null;

        String countryCode = StringUtils.hasText(request.getCountryCode()) ? request.getCountryCode() : "+91";

        boolean isSecure = Boolean.TRUE.equals(request.getSecureAccount())
                || Boolean.TRUE.equals(request.getTwoStepEnabled());

        if (!isSecure && !StringUtils.hasText(request.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required when two-step verification is not enabled");
        }

        // 1. Create inactive and unverified account
        AccountEntity account = AccountEntity.builder()
                .phoneNumber(resolvedPhone)
                .countryCode(countryCode)
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .isVerified(false)
                .isActive(false)
                .secureAccount(isSecure)
                .build();

        AccountEntity savedAccount = accountRepository.save(account);

        // 2. Generate secure activation token and persist in DB
        String activationToken = UUID.randomUUID().toString();
        int ttlHours = authProperties.getActivationTokenTtlHours() > 0 ? authProperties.getActivationTokenTtlHours() : 24;
        AccountActivationEntity activation = AccountActivationEntity.builder()
                .accountId(savedAccount.getAccountId())
                .token(activationToken)
                .expiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS))
                .build();
        accountActivationRepository.save(activation);

        // 3. Send welcome email with activation link
        String activationLink = authProperties.getActivationBaseUrl() + "?token=" + activationToken;
        authEmailService.sendWelcomeAndActivationEmail(savedAccount.getEmail(), activationLink);

        // Account is not active yet; do not issue JWT until activated
        return AuthResponse.builder()
                .token(null)
                .refreshToken(null)
                .account(toDto(savedAccount))
                .build();
    }

    @Transactional
    public AccountDto activateAccount(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activation token is required");
        }

        AccountActivationEntity activation = accountActivationRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid activation token"));

        if (activation.getUsedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Account activation link has already been used");
        }

        if (Instant.now().isAfter(activation.getExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Account activation link has expired");
        }

        AccountEntity account = accountRepository.findById(activation.getAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account associated with activation token not found"));

        account.setIsActive(true);
        AccountEntity updatedAccount = accountRepository.save(account);

        activation.setUsedAt(Instant.now());
        accountActivationRepository.save(activation);

        log.info("[Auth Service] Account '{}' ({}) successfully activated via email link",
                updatedAccount.getAccountId(), updatedAccount.getEmail());

        return toDto(updatedAccount);
    }

    public LoginResponse login(LoginRequest request) {
        String resolvedPhone = resolveDestinationPhone(request.getPhoneNumber(), request.getCountryCode());
        String destination = StringUtils.hasText(resolvedPhone)
                ? resolvedPhone
                : request.getEmail();

        if (!StringUtils.hasText(destination)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number or email is required");
        }

        // Check if account exists
        Optional<AccountEntity> accountOpt = StringUtils.hasText(resolvedPhone)
                ? accountRepository.findByPhoneNumber(resolvedPhone)
                : accountRepository.findByEmail(request.getEmail());

        AccountEntity account = accountOpt.orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/phone or password"));

        if (Boolean.FALSE.equals(account.getIsActive())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Account is not active. Please activate your account via the link sent to your email.");
        }

        boolean isSecureAccount = Boolean.TRUE.equals(account.getSecureAccount());

        if (isSecureAccount) {
            // Two-step enabled -> OTP flow
            otpService.generateOtp(destination, request.getProvider());
            log.info("[Auth Service] Two-step OTP challenge dispatched for secure account '{}' to '{}'",
                    account.getAccountId(), destination);

            return LoginResponse.builder()
                    .success(true)
                    .message("OTP sent successfully. Please verify OTP to complete login.")
                    .requiresOtp(true)
                    .account(toDto(account))
                    .build();
        } else {
            // Email/phone with password login
            if (!StringUtils.hasText(request.getPassword())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required for password login");
            }

            if (!StringUtils.hasText(account.getPasswordHash())
                    || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }

            String token = jwtTokenProvider.generateToken(account.getAccountId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(account.getAccountId());

            log.info("[Auth Service] Password login successful for account '{}' ({})",
                    account.getAccountId(), destination);

            return LoginResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .requiresOtp(false)
                    .token(token)
                    .refreshToken(refreshToken)
                    .account(toDto(account))
                    .build();
        }
    }

    public void sendOtp(SendOtpRequest request) {
        String resolvedPhone = resolveDestinationPhone(request.getPhoneNumber(), request.getCountryCode());
        String destination = StringUtils.hasText(resolvedPhone)
                ? resolvedPhone
                : request.getEmail();

        if (!StringUtils.hasText(destination)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number or email is required");
        }

        // Check active status if account already exists
        Optional<AccountEntity> accountOpt = StringUtils.hasText(resolvedPhone)
                ? accountRepository.findByPhoneNumber(resolvedPhone)
                : accountRepository.findByEmail(request.getEmail());

        if (accountOpt.isPresent() && Boolean.FALSE.equals(accountOpt.get().getIsActive())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Account is not active. Please activate your account via the link sent to your email.");
        }

        // Trigger OTP delivery with optional provider override
        otpService.generateOtp(destination, request.getProvider());
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String resolvedPhone = resolveDestinationPhone(request.getPhoneNumber(), request.getCountryCode());
        String destination = StringUtils.hasText(resolvedPhone)
                ? resolvedPhone
                : request.getEmail();

        if (!StringUtils.hasText(destination)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number or email is required");
        }

        boolean isValid = otpService.verifyOtp(destination, request.getOtp());
        if (!isValid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP code");
        }

        // Find existing account or create on-the-fly (seamless onboarding)
        Optional<AccountEntity> accountOpt = StringUtils.hasText(resolvedPhone)
                ? accountRepository.findByPhoneNumber(resolvedPhone)
                : accountRepository.findByEmail(request.getEmail());

        if (accountOpt.isPresent() && Boolean.FALSE.equals(accountOpt.get().getIsActive())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Account is not active. Please activate your account via the link sent to your email.");
        }

        String countryCode = StringUtils.hasText(request.getCountryCode()) ? request.getCountryCode() : "+1";

        AccountEntity account = accountOpt.orElseGet(() -> {
            AccountEntity newAccount = AccountEntity.builder()
                    .phoneNumber(resolvedPhone)
                    .countryCode(countryCode)
                    .email(request.getEmail())
                    .isVerified(true)
                    .isActive(true)
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
        Instant created = entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now();
        Instant updated = entity.getUpdatedAt() != null ? entity.getUpdatedAt() : created;

        return AccountDto.builder()
                .accountId(entity.getAccountId())
                .phoneNumber(entity.getPhoneNumber())
                .countryCode(entity.getCountryCode())
                .email(entity.getEmail())
                .isVerified(entity.getIsVerified())
                .isActive(entity.getIsActive())
                .secureAccount(Boolean.TRUE.equals(entity.getSecureAccount()))
                .createdAt(created.toString())
                .updatedAt(updated.toString())
                .build();
    }
}
