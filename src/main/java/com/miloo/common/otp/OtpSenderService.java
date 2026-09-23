package com.miloo.common.otp;

/**
 * Standard interface for sending OTP messages across diverse channels and third-party gateways.
 */
public interface OtpSenderService {

    /**
     * Dispatches the OTP code to the target destination (phone number or email address).
     *
     * @param destination Phone number (e.g. +1234567890) or email address
     * @param otpCode     Numeric verification code
     */
    void sendOtp(String destination, String otpCode);

    /**
     * Returns the specific provider type represented by this service (e.g. TWILIO, MSG91, SMTP).
     */
    OtpProvider getProviderType();

    /**
     * Returns the channel supported by this service (SMS or EMAIL).
     */
    OtpChannel getChannel();
}
