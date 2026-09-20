package com.miloo.modules.auth.service;

import com.miloo.common.otp.OtpChannel;
import com.miloo.common.otp.OtpProperties;
import com.miloo.common.otp.OtpProvider;
import com.miloo.common.otp.OtpSenderFactory;
import com.miloo.common.otp.OtpSenderService;
import com.miloo.common.otp.OtpStatus;
import com.miloo.modules.auth.entity.OtpEntity;
import com.miloo.modules.auth.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private OtpRepository otpRepository;
    private OtpSenderFactory otpSenderFactory;
    private OtpSenderService mockSender;
    private OtpProperties properties;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpRepository = mock(OtpRepository.class);
        otpSenderFactory = mock(OtpSenderFactory.class);
        mockSender = mock(OtpSenderService.class);
        when(mockSender.getProviderType()).thenReturn(OtpProvider.TWILIO);

        when(otpSenderFactory.getSenderForDestination(any(), any())).thenReturn(mockSender);
        when(otpSenderFactory.getSenderForDestination(any())).thenReturn(mockSender);

        properties = new OtpProperties();
        properties.setTtlMinutes(5);
        properties.setMaxAttempts(5);
        properties.setMockEnabled(true);

        otpService = new OtpService(otpRepository, otpSenderFactory, properties);
    }

    @Test
    @DisplayName("generateOtp stores OTP in DB with 5 min TTL and invalidates previous pending OTPs")
    void testGenerateOtp_success() {
        String destination = "+15005550006";

        String code = otpService.generateOtp(destination);

        assertNotNull(code);
        assertEquals(6, code.length());

        // Verify invalidation of previous pending OTPs
        verify(otpRepository).updateStatusByDestinationAndStatus(destination, OtpStatus.PENDING, OtpStatus.EXPIRED);

        // Verify save to DB
        ArgumentCaptor<OtpEntity> captor = ArgumentCaptor.forClass(OtpEntity.class);
        verify(otpRepository).save(captor.capture());

        OtpEntity saved = captor.getValue();
        assertEquals(destination, saved.getDestination());
        assertEquals(code, saved.getOtpCode());
        assertEquals(OtpChannel.SMS, saved.getChannel());
        assertEquals(OtpProvider.TWILIO, saved.getProvider());
        assertEquals(OtpStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttempts());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now().plus(4, ChronoUnit.MINUTES)));
        assertTrue(saved.getExpiresAt().isBefore(Instant.now().plus(6, ChronoUnit.MINUTES)));

        // Verify dispatch via sender
        verify(mockSender).sendOtp(destination, code);
    }

    @Test
    @DisplayName("verifyOtp successfully validates correct OTP and marks status as VERIFIED")
    void testVerifyOtp_success() {
        String destination = "+15005550006";
        String code = "654321";

        OtpEntity activeOtp = OtpEntity.builder()
                .otpId(UUID.randomUUID())
                .destination(destination)
                .otpCode(code)
                .channel(OtpChannel.SMS)
                .provider(OtpProvider.TWILIO)
                .status(OtpStatus.PENDING)
                .attempts(0)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();

        when(otpRepository.findTopByDestinationAndStatusOrderByCreatedAtDesc(destination, OtpStatus.PENDING))
                .thenReturn(Optional.of(activeOtp));

        boolean result = otpService.verifyOtp(destination, code);

        assertTrue(result);
        assertEquals(OtpStatus.VERIFIED, activeOtp.getStatus());
        verify(otpRepository).save(activeOtp);
    }

    @Test
    @DisplayName("verifyOtp rejects incorrect OTP and increments attempt count")
    void testVerifyOtp_incorrectCode() {
        String destination = "+15005550006";
        String correctCode = "654321";

        // Turn mockEnabled off to strictly test incorrect code rejection without master demo fallback
        properties.setMockEnabled(false);

        OtpEntity activeOtp = OtpEntity.builder()
                .otpId(UUID.randomUUID())
                .destination(destination)
                .otpCode(correctCode)
                .channel(OtpChannel.SMS)
                .provider(OtpProvider.TWILIO)
                .status(OtpStatus.PENDING)
                .attempts(1)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();

        when(otpRepository.findTopByDestinationAndStatusOrderByCreatedAtDesc(destination, OtpStatus.PENDING))
                .thenReturn(Optional.of(activeOtp));

        boolean result = otpService.verifyOtp(destination, "999999");

        assertFalse(result);
        assertEquals(2, activeOtp.getAttempts());
        assertEquals(OtpStatus.PENDING, activeOtp.getStatus());
        verify(otpRepository).save(activeOtp);
    }

    @Test
    @DisplayName("verifyOtp marks OTP as FAILED when max attempts exceeded")
    void testVerifyOtp_maxAttemptsExceeded() {
        String destination = "+15005550006";
        properties.setMockEnabled(false);
        properties.setMaxAttempts(3);

        OtpEntity activeOtp = OtpEntity.builder()
                .otpId(UUID.randomUUID())
                .destination(destination)
                .otpCode("112233")
                .channel(OtpChannel.SMS)
                .provider(OtpProvider.TWILIO)
                .status(OtpStatus.PENDING)
                .attempts(2)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();

        when(otpRepository.findTopByDestinationAndStatusOrderByCreatedAtDesc(destination, OtpStatus.PENDING))
                .thenReturn(Optional.of(activeOtp));

        boolean result = otpService.verifyOtp(destination, "000000");

        assertFalse(result);
        assertEquals(3, activeOtp.getAttempts());
        assertEquals(OtpStatus.FAILED, activeOtp.getStatus());
        verify(otpRepository).save(activeOtp);
    }

    @Test
    @DisplayName("verifyOtp rejects and marks expired OTP as EXPIRED when 5 min TTL elapsed")
    void testVerifyOtp_expiredTtl() {
        String destination = "+15005550006";

        OtpEntity expiredOtp = OtpEntity.builder()
                .otpId(UUID.randomUUID())
                .destination(destination)
                .otpCode("112233")
                .channel(OtpChannel.SMS)
                .provider(OtpProvider.TWILIO)
                .status(OtpStatus.PENDING)
                .attempts(0)
                .expiresAt(Instant.now().minus(10, ChronoUnit.SECONDS)) // expired 10 seconds ago
                .build();

        when(otpRepository.findTopByDestinationAndStatusOrderByCreatedAtDesc(destination, OtpStatus.PENDING))
                .thenReturn(Optional.of(expiredOtp));

        boolean result = otpService.verifyOtp(destination, "112233");

        assertFalse(result);
        assertEquals(OtpStatus.EXPIRED, expiredOtp.getStatus());
        verify(otpRepository).save(expiredOtp);
    }

    @Test
    @DisplayName("verifyOtp accepts master demo OTP in mock mode even when DB has no pending OTP")
    void testVerifyOtp_masterDemoMockMode() {
        String destination = "+15005550006";
        properties.setMockEnabled(true);

        when(otpRepository.findTopByDestinationAndStatusOrderByCreatedAtDesc(destination, OtpStatus.PENDING))
                .thenReturn(Optional.empty());

        boolean result = otpService.verifyOtp(destination, "123456");
        assertTrue(result);
    }
}
