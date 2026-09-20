package com.miloo.modules.auth.service;

import com.miloo.common.exception.ApiException;
import com.miloo.common.otp.OtpProperties;
import com.miloo.config.AuthProperties;
import com.miloo.modules.auth.constant.AuthConstants;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthEmailService {

    private final JavaMailSender mailSender;
    private final OtpProperties properties;
    private final AuthProperties authProperties;

    private boolean isDummy(String val) {
        return val == null || val.isBlank() || val.contains("dummy");
    }

    /**
     * Sends a welcome email containing the account activation link.
     *
     * @param recipientEmail User's registered email address
     * @param activationLink Full activation URL
     */
    public void sendWelcomeAndActivationEmail(String recipientEmail, String activationLink) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        OtpProperties.SmtpProperties smtp = properties.getSmtp();

        if (authProperties.isMockEmailEnabled() || isDummy(smtp.getUsername()) || isDummy(smtp.getPassword())) {
            log.info("[Welcome Email Mock] Sent activation email to '{}' with link: '{}'",
                    recipientEmail, activationLink);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(recipientEmail);
            helper.setFrom(smtp.getFromEmail(), smtp.getFromName());
            helper.setSubject("Welcome to Miloo! Activate Your Account");

            String html = AuthConstants.buildWelcomeActivationEmail(activationLink, 24);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("[Welcome Email] Successfully sent activation email to '{}'", recipientEmail);
        } catch (Exception e) {
            log.error("[Welcome Email] Failed to send activation email to '{}': {}", recipientEmail, e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to send welcome email: " + e.getMessage());
        }
    }
}
