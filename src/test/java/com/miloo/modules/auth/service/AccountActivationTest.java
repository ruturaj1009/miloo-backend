package com.miloo.modules.auth.service;

import com.miloo.common.exception.ApiException;
import com.miloo.config.AuthProperties;
import com.miloo.config.JwtAuthFilter;
import com.miloo.config.JwtTokenProvider;
import com.miloo.modules.auth.constant.AuthConstants;
import com.miloo.modules.auth.controller.AuthController;
import com.miloo.modules.auth.dto.*;
import com.miloo.modules.auth.entity.AccountActivationEntity;
import com.miloo.modules.auth.entity.AccountEntity;
import com.miloo.modules.auth.repository.AccountActivationRepository;
import com.miloo.modules.auth.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AccountActivationTest {

    private AccountRepository accountRepository;
    private AccountActivationRepository accountActivationRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private OtpService otpService;
    private AuthEmailService authEmailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountActivationRepository = mock(AccountActivationRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        otpService = mock(OtpService.class);
        authEmailService = mock(AuthEmailService.class);

        when(passwordEncoder.encode(any())).thenReturn("hashed_password");

        AuthProperties authProperties = new AuthProperties();
        authProperties.setActivationBaseUrl("http://localhost:8080/api/v1/auth/activate");
        authProperties.setActivationTokenTtlHours(24);

        authService = new AuthService(
                accountRepository,
                accountActivationRepository,
                passwordEncoder,
                jwtTokenProvider,
                otpService,
                authEmailService,
                authProperties
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("register creates inactive account, persists activation token, and dispatches welcome email")
    void testRegister_createsInactiveAccount_andDispatchesEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .phoneNumber("+15550001122")
                .email("newuser@miloo.app")
                .password("Password123!")
                .build();

        when(accountRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);

        UUID generatedAccountId = UUID.randomUUID();
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity entity = invocation.getArgument(0);
            entity.setAccountId(generatedAccountId);
            return entity;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNull(response.getToken(), "Token must be null until account is activated");
        assertNotNull(response.getAccount());
        assertFalse(response.getAccount().getIsActive(), "Account must be inactive upon registration");
        assertFalse(response.getAccount().getIsVerified(), "Account must be unverified upon registration");
        assertNotNull(response.getAccount().getCreatedAt(), "Account created_at must not be null");
        assertNotNull(response.getAccount().getUpdatedAt(), "Account updated_at must not be null");

        // Verify activation token saved in DB
        ArgumentCaptor<AccountActivationEntity> tokenCaptor = ArgumentCaptor.forClass(AccountActivationEntity.class);
        verify(accountActivationRepository).save(tokenCaptor.capture());
        AccountActivationEntity savedActivation = tokenCaptor.getValue();
        assertEquals(generatedAccountId, savedActivation.getAccountId());
        assertNotNull(savedActivation.getToken());
        assertTrue(savedActivation.getExpiresAt().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));

        // Verify welcome email dispatched with activation link
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(authEmailService).sendWelcomeAndActivationEmail(eq("newuser@miloo.app"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().contains("http://localhost:8080/api/v1/auth/activate?token="));
        assertTrue(linkCaptor.getValue().contains(savedActivation.getToken()));
    }

    @Test
    @DisplayName("activateAccount updates is_active and is_verified to true and marks token as used")
    void testActivateAccount_success() {
        String token = "valid-activation-token";
        UUID accountId = UUID.randomUUID();

        AccountActivationEntity activation = AccountActivationEntity.builder()
                .activationId(UUID.randomUUID())
                .accountId(accountId)
                .token(token)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .usedAt(null)
                .build();

        AccountEntity account = AccountEntity.builder()
                .accountId(accountId)
                .email("user@miloo.app")
                .phoneNumber("+15551234567")
                .isActive(false)
                .isVerified(false)
                .build();

        when(accountActivationRepository.findByToken(token)).thenReturn(Optional.of(activation));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDto activatedDto = authService.activateAccount(token);

        assertTrue(activatedDto.getIsActive());
        assertFalse(activatedDto.getIsVerified(), "Activation activates account without marking is_verified");
        assertNotNull(activation.getUsedAt(), "Token must be marked as used");
        verify(accountActivationRepository).save(activation);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("activateAccount rejects already used activation token")
    void testActivateAccount_alreadyUsed() {
        String token = "already-used-token";

        AccountActivationEntity activation = AccountActivationEntity.builder()
                .token(token)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(accountActivationRepository.findByToken(token)).thenReturn(Optional.of(activation));

        ApiException ex = assertThrows(ApiException.class, () -> authService.activateAccount(token));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    @DisplayName("activateAccount rejects expired activation token")
    void testActivateAccount_expired() {
        String token = "expired-token";

        AccountActivationEntity activation = AccountActivationEntity.builder()
                .token(token)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .usedAt(null)
                .build();

        when(accountActivationRepository.findByToken(token)).thenReturn(Optional.of(activation));

        ApiException ex = assertThrows(ApiException.class, () -> authService.activateAccount(token));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("login rejects inactive accounts with 403 Forbidden")
    void testLogin_inactiveAccount_blocked() {
        LoginRequest request = LoginRequest.builder()
                .email("inactive@miloo.app")
                .build();

        AccountEntity inactiveAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .email("inactive@miloo.app")
                .isActive(false)
                .build();

        when(accountRepository.findByEmail("inactive@miloo.app")).thenReturn(Optional.of(inactiveAccount));

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Account is not active"));

        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("login proceeds with OTP dispatch for active accounts when secureAccount is true")
    void testLogin_activeAccount_allowed() {
        LoginRequest request = LoginRequest.builder()
                .email("active@miloo.app")
                .build();

        AccountEntity activeAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .email("active@miloo.app")
                .isActive(true)
                .secureAccount(true)
                .build();

        when(accountRepository.findByEmail("active@miloo.app")).thenReturn(Optional.of(activeAccount));

        LoginResponse response = authService.login(request);
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertTrue(response.getRequiresOtp(), "Requires OTP must be true when secureAccount is enabled");
        assertNull(response.getToken(), "Token must be null until OTP verification");
        verify(otpService).generateOtp("active@miloo.app", null);
    }

    @Test
    @DisplayName("JwtAuthFilter blocks authentication when account is not active")
    void testJwtAuthFilter_inactiveAccount_blocksAuth() throws Exception {
        JwtTokenProvider mockTokenProvider = mock(JwtTokenProvider.class);
        AccountRepository mockAccountRepo = mock(AccountRepository.class);
        JwtAuthFilter filter = new JwtAuthFilter(mockTokenProvider, mockAccountRepo);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        UUID userId = UUID.randomUUID();
        when(req.getHeader("Authorization")).thenReturn("Bearer mock.jwt.token");
        when(mockTokenProvider.validateToken("mock.jwt.token")).thenReturn(true);
        when(mockTokenProvider.getUserIdFromToken("mock.jwt.token")).thenReturn(userId);

        // Account is inactive
        AccountEntity inactiveAccount = AccountEntity.builder()
                .accountId(userId)
                .isActive(false)
                .build();
        when(mockAccountRepo.findById(userId)).thenReturn(Optional.of(inactiveAccount));

        filter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must NOT be set in SecurityContextHolder for inactive user");
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("JwtAuthFilter sets authentication when account is active")
    void testJwtAuthFilter_activeAccount_setsAuth() throws Exception {
        JwtTokenProvider mockTokenProvider = mock(JwtTokenProvider.class);
        AccountRepository mockAccountRepo = mock(AccountRepository.class);
        JwtAuthFilter filter = new JwtAuthFilter(mockTokenProvider, mockAccountRepo);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        UUID userId = UUID.randomUUID();
        when(req.getHeader("Authorization")).thenReturn("Bearer mock.jwt.token");
        when(mockTokenProvider.validateToken("mock.jwt.token")).thenReturn(true);
        when(mockTokenProvider.getUserIdFromToken("mock.jwt.token")).thenReturn(userId);

        // Account is active
        AccountEntity activeAccount = AccountEntity.builder()
                .accountId(userId)
                .isActive(true)
                .build();
        when(mockAccountRepo.findById(userId)).thenReturn(Optional.of(activeAccount));

        filter.doFilter(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication MUST be set in SecurityContextHolder for active user");
        assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("register with phone and countryCode properly formats E.164 and stores countryCode")
    void testRegister_withCountryCode_formatsE164Phone() {
        RegisterRequest request = RegisterRequest.builder()
                .phoneNumber("9876543210")
                .countryCode("+91")
                .email("indiauser@miloo.app")
                .password("Password123!")
                .build();

        when(accountRepository.existsByPhoneNumber("+919876543210")).thenReturn(false);
        when(accountRepository.existsByEmail("indiauser@miloo.app")).thenReturn(false);

        ArgumentCaptor<AccountEntity> accountCaptor = ArgumentCaptor.forClass(AccountEntity.class);
        when(accountRepository.save(accountCaptor.capture())).thenAnswer(invocation -> {
            AccountEntity entity = invocation.getArgument(0);
            entity.setAccountId(UUID.randomUUID());
            return entity;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        AccountEntity savedAccount = accountCaptor.getValue();
        assertEquals("+919876543210", savedAccount.getPhoneNumber(), "Phone must be normalized to E.164");
        assertEquals("+91", savedAccount.getCountryCode(), "Country code must be saved");
        assertEquals("+91", response.getAccount().getCountryCode());
        assertEquals("+919876543210", response.getAccount().getPhoneNumber());
    }

    @Test
    @DisplayName("login with phone and countryCode without leading plus normalizes properly")
    void testLogin_withCountryCodeWithoutPlus_normalizesPhone() {
        LoginRequest request = LoginRequest.builder()
                .phoneNumber("9876543210")
                .countryCode("91")
                .build();

        AccountEntity activeAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .phoneNumber("+919876543210")
                .countryCode("+91")
                .isActive(true)
                .secureAccount(true)
                .build();

        when(accountRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(activeAccount));

        LoginResponse response = authService.login(request);
        assertNotNull(response);
        assertTrue(response.getRequiresOtp());
        verify(otpService).generateOtp("+919876543210", null);
    }

    @Test
    @DisplayName("resolveDestinationPhone correctly formats various phone and countryCode combinations")
    void testResolveDestinationPhone_combinations() {
        // Already has plus
        assertEquals("+15551234567", authService.resolveDestinationPhone("+15551234567", "+1"));
        assertEquals("+919876543210", authService.resolveDestinationPhone("+919876543210", null));

        // Missing plus on country code
        assertEquals("+919876543210", authService.resolveDestinationPhone("9876543210", "91"));
        assertEquals("+447123456789", authService.resolveDestinationPhone("7123456789", "+44"));

        // No country code
        assertEquals("9876543210", authService.resolveDestinationPhone("9876543210", null));
        assertEquals("9876543210", authService.resolveDestinationPhone("9876543210", ""));

        // Null phone
        assertNull(authService.resolveDestinationPhone(null, "+1"));
        assertNull(authService.resolveDestinationPhone("", "+1"));
    }

    @Test
    @DisplayName("register persists secureAccount flag when enabled")
    void testRegister_withSecureAccount_flag() {
        RegisterRequest request = RegisterRequest.builder()
                .phoneNumber("+15559998877")
                .email("twofactor@miloo.app")
                .password("Password123!")
                .secureAccount(true)
                .build();

        when(accountRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        when(accountRepository.save(captor.capture())).thenAnswer(inv -> {
            AccountEntity entity = inv.getArgument(0);
            entity.setAccountId(UUID.randomUUID());
            return entity;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(captor.getValue().getSecureAccount());
        assertTrue(response.getAccount().getSecureAccount());
    }

    @Test
    @DisplayName("login with password succeeds directly when secureAccount is false")
    void testLogin_passwordLogin_success() {
        UUID accountId = UUID.randomUUID();
        LoginRequest request = LoginRequest.builder()
                .email("passworduser@miloo.app")
                .password("CorrectPassword123!")
                .build();

        AccountEntity account = AccountEntity.builder()
                .accountId(accountId)
                .email("passworduser@miloo.app")
                .passwordHash("hashed_password")
                .isActive(true)
                .secureAccount(false)
                .build();

        when(accountRepository.findByEmail("passworduser@miloo.app")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("CorrectPassword123!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(accountId)).thenReturn("jwt.token.123");
        when(jwtTokenProvider.generateRefreshToken(accountId)).thenReturn("refresh.token.123");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertFalse(response.getRequiresOtp(), "OTP must NOT be required for password login");
        assertEquals("jwt.token.123", response.getToken());
        assertEquals("refresh.token.123", response.getRefreshToken());
        assertNotNull(response.getAccount());
        assertFalse(response.getAccount().getSecureAccount());

        // Ensure NO OTP was dispatched
        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("login with incorrect password throws 401 Unauthorized when secureAccount is false")
    void testLogin_passwordLogin_wrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("passworduser@miloo.app")
                .password("WrongPassword")
                .build();

        AccountEntity account = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .email("passworduser@miloo.app")
                .passwordHash("hashed_password")
                .isActive(true)
                .secureAccount(false)
                .build();

        when(accountRepository.findByEmail("passworduser@miloo.app")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPassword", "hashed_password")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid credentials"));
        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("login without password throws 400 Bad Request when secureAccount is false")
    void testLogin_passwordLogin_missingPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("passworduser@miloo.app")
                .build();

        AccountEntity account = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .email("passworduser@miloo.app")
                .passwordHash("hashed_password")
                .isActive(true)
                .secureAccount(false)
                .build();

        when(accountRepository.findByEmail("passworduser@miloo.app")).thenReturn(Optional.of(account));

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Password is required for password login"));
        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("GET /activate returns styled HTML error page when token is already used in a web browser")
    void testActivate_alreadyUsed_returnsHtmlErrorInBrowser() {
        AuthController controller = new AuthController(authService);
        String token = "already-used-token";

        AccountActivationEntity activation = AccountActivationEntity.builder()
                .token(token)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(accountActivationRepository.findByToken(token)).thenReturn(Optional.of(activation));

        ResponseEntity<?> response = controller.activate(token, "text/html,application/xhtml+xml");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertTrue(response.getBody().toString().contains("Link Already Used"));
        assertTrue(response.getBody().toString().contains("already been used"));
    }

    @Test
    @DisplayName("GET /activate returns styled HTML error page when token is expired in a web browser")
    void testActivate_expired_returnsHtmlErrorInBrowser() {
        AuthController controller = new AuthController(authService);
        String token = "expired-token";

        AccountActivationEntity activation = AccountActivationEntity.builder()
                .token(token)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .usedAt(null)
                .build();

        when(accountActivationRepository.findByToken(token)).thenReturn(Optional.of(activation));

        ResponseEntity<?> response = controller.activate(token, "text/html");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertTrue(response.getBody().toString().contains("Activation Link Expired"));
        assertTrue(response.getBody().toString().contains("expired"));
    }

    @Test
    @DisplayName("GET /activate rethrows ApiException for API clients expecting JSON")
    void testActivate_alreadyUsed_rethrowsForJsonClients() {
        AuthController controller = new AuthController(authService);
        String token = "already-used-token";

        AccountActivationEntity activation = AccountActivationEntity.builder()
                .token(token)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(accountActivationRepository.findByToken(token)).thenReturn(Optional.of(activation));

        ApiException ex = assertThrows(ApiException.class, () ->
                controller.activate(token, "application/json")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    @DisplayName("AuthConstants.buildActivationErrorHtml correctly customizes title and description")
    void testBuildActivationErrorHtml_customizations() {
        String usedHtml = AuthConstants.buildActivationErrorHtml("Account activation link has already been used");
        assertTrue(usedHtml.contains("Link Already Used"));
        assertTrue(usedHtml.contains("already been used"));

        String expiredHtml = AuthConstants.buildActivationErrorHtml("Account activation link has expired");
        assertTrue(expiredHtml.contains("Activation Link Expired"));
        assertTrue(expiredHtml.contains("expired"));

        String genericHtml = AuthConstants.buildActivationErrorHtml("Invalid activation token");
        assertTrue(genericHtml.contains("Activation Failed"));
        assertTrue(genericHtml.contains("Invalid activation token"));
    }
}
