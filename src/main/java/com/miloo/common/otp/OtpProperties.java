package com.miloo.common.otp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {

    /**
     * OTP validity Time-To-Live in minutes (default 5 minutes).
     */
    private int ttlMinutes = 5;

    /**
     * Whether mock mode is enabled (logs OTP instead of calling live third-party gateways).
     */
    private boolean mockEnabled = true;

    /**
     * Active mobile SMS provider: twilio | msg91
     */
    private String smsProvider = "twilio";

    /**
     * Active email provider: smtp
     */
    private String emailProvider = "smtp";

    /**
     * Maximum allowed verification attempts before marking OTP as FAILED.
     */
    private int maxAttempts = 5;

    private TwilioProperties twilio = new TwilioProperties();
    private Msg91Properties msg91 = new Msg91Properties();
    private SmtpProperties smtp = new SmtpProperties();

    public OtpProvider getSmsProviderEnum() {
        return OtpProvider.fromString(smsProvider);
    }

    public OtpProvider getEmailProviderEnum() {
        return OtpProvider.fromString(emailProvider);
    }

    @Data
    public static class TwilioProperties {
        private String accountSid = "dummy_sid";
        private String authToken = "dummy_token";
        private String fromPhoneNumber = "+15005550006";
    }

    @Data
    public static class Msg91Properties {
        private String authKey = "dummy_key";
        private String templateId = "dummy_template";
        private String senderId = "MILOO";
    }

    @Data
    public static class SmtpProperties {
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username = "dummy_user";
        private String password = "dummy_password";
        private String fromEmail = "noreply@miloo.app";
        private String fromName = "Miloo Dating";
        private boolean auth = true;
        private boolean starttls = true;
    }
}
