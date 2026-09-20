package com.miloo.modules.auth.service;

import com.miloo.common.otp.OtpProperties;
import com.miloo.config.AuthProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthEmailServiceTest {

    private JavaMailSender mailSender;
    private OtpProperties otpProperties;
    private AuthProperties authProperties;
    private AuthEmailService authEmailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        otpProperties = new OtpProperties();
        otpProperties.getSmtp().setUsername("info.izyhealth@gmail.com");
        otpProperties.getSmtp().setPassword("real_app_password");
        otpProperties.getSmtp().setFromEmail("info.izyhealth@gmail.com");

        authProperties = new AuthProperties();
        authProperties.setMockEmailEnabled(false);

        authEmailService = new AuthEmailService(mailSender, otpProperties, authProperties);
    }

    @Test
    @DisplayName("Activation email sends real email when authProperties.isMockEmailEnabled is false, even if otpProperties.isMockEnabled is true")
    void testSendWelcomeEmail_realSending_independentFromOtpMock() {
        // OTP service is in mock mode
        otpProperties.setMockEnabled(true);
        // Activation email is NOT in mock mode
        authProperties.setMockEmailEnabled(false);

        assertDoesNotThrow(() ->
                authEmailService.sendWelcomeAndActivationEmail("user@example.com", "http://localhost:8080/api/v1/auth/activate?token=test")
        );

        // mailSender.send MUST be called because activation email mock is false
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Activation email is mocked when authProperties.isMockEmailEnabled is true, even if otpProperties.isMockEnabled is false")
    void testSendWelcomeEmail_mockEnabled_independentFromOtpMock() {
        // OTP service is NOT in mock mode
        otpProperties.setMockEnabled(false);
        // Activation email IS in mock mode
        authProperties.setMockEmailEnabled(true);

        assertDoesNotThrow(() ->
                authEmailService.sendWelcomeAndActivationEmail("user@example.com", "http://localhost:8080/api/v1/auth/activate?token=test")
        );

        // mailSender.send must NOT be called
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Activation email falls back to mock if SMTP credentials are dummy")
    void testSendWelcomeEmail_dummyCredentials_fallsBackToMock() {
        otpProperties.getSmtp().setUsername("dummy@gmail.com");
        otpProperties.getSmtp().setPassword("dummy_password");
        authProperties.setMockEmailEnabled(false);

        assertDoesNotThrow(() ->
                authEmailService.sendWelcomeAndActivationEmail("user@example.com", "http://localhost:8080/api/v1/auth/activate?token=test")
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
