package com.miloo.common.otp;

public enum OtpProvider {
    TWILIO,
    MSG91,
    SMTP,
    MOCK;

    public static OtpProvider fromString(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return TWILIO;
        }
        try {
            return OtpProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TWILIO;
        }
    }
}
