package com.miloo.common.otp.impl;

import com.miloo.common.exception.ApiException;
import com.miloo.common.otp.OtpChannel;
import com.miloo.common.otp.OtpProperties;
import com.miloo.common.otp.OtpProvider;
import com.miloo.common.otp.OtpSenderService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service("smtpEmailOtpSenderService")
@RequiredArgsConstructor
public class SmtpEmailOtpSenderService implements OtpSenderService {

    private final OtpProperties properties;
    private final JavaMailSender mailSender;

    private boolean isDummy(String val) {
        return val == null || val.isBlank() || val.contains("dummy");
    }

    @Override
    public void sendOtp(String destination, String otpCode) {
        OtpProperties.SmtpProperties smtp = properties.getSmtp();

        if (properties.isMockEnabled() || isDummy(smtp.getUsername()) || isDummy(smtp.getPassword())) {
            log.info("[SMTP Mock] Email OTP '{}' dispatched to destination '{}' (From: {} <{}>)",
                    otpCode, destination, smtp.getFromName(), smtp.getFromEmail());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(destination);
            helper.setFrom(smtp.getFromEmail(), smtp.getFromName());
            helper.setSubject("Your Miloo Verification Code: " + otpCode);

            String htmlContent = "<div style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 30px; border: 1px solid #eaeaea; border-radius: 12px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #E94057; text-align: center; margin-bottom: 24px;\">Miloo Verification</h2>"
                    + "<p style=\"font-size: 16px; color: #333333; line-height: 1.5;\">Hello,</p>"
                    + "<p style=\"font-size: 16px; color: #333333; line-height: 1.5;\">Your one-time verification code is:</p>"
                    + "<div style=\"background: #FDF1F3; padding: 18px; border-radius: 8px; text-align: center; margin: 24px 0;\">"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #E94057;\">" + otpCode + "</span>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #666666; line-height: 1.5;\">This code is valid for <strong>" + properties.getTtlMinutes() + " minutes</strong>. Do not share this code with anyone.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #f0f0f0; margin: 25px 0;\">"
                    + "<p style=\"font-size: 12px; color: #999999; text-align: center;\">&copy; 2026 Miloo App. All rights reserved.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("[SMTP] Successfully sent verification email to '{}'", destination);
        } catch (Exception e) {
            log.error("[SMTP] Failed to send verification email to '{}': {}", destination, e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to send email OTP via SMTP: " + e.getMessage());
        }
    }

    @Override
    public OtpProvider getProviderType() {
        return OtpProvider.SMTP;
    }

    @Override
    public OtpChannel getChannel() {
        return OtpChannel.EMAIL;
    }
}
