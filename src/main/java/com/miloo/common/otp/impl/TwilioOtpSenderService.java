package com.miloo.common.otp.impl;

import com.miloo.common.exception.ApiException;
import com.miloo.common.otp.OtpChannel;
import com.miloo.common.otp.OtpProperties;
import com.miloo.common.otp.OtpProvider;
import com.miloo.common.otp.OtpSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service("twilioOtpSenderService")
@RequiredArgsConstructor
public class TwilioOtpSenderService implements OtpSenderService {

    private final OtpProperties properties;
    private final RestClient.Builder restClientBuilder;

    private boolean isDummy(String val) {
        return val == null || val.isBlank() || val.contains("dummy");
    }

    @Override
    public void sendOtp(String destination, String otpCode) {
        OtpProperties.TwilioProperties twilio = properties.getTwilio();

        if (properties.isMockEnabled() || isDummy(twilio.getAccountSid()) || isDummy(twilio.getAuthToken())) {
            log.info("[Twilio Mock] SMS OTP '{}' dispatched to destination '{}' (From: {})",
                    otpCode, destination, twilio.getFromPhoneNumber());
            return;
        }

        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilio.getAccountSid());
            String auth = twilio.getAccountSid() + ":" + twilio.getAuthToken();
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("To", destination);
            formData.add("From", twilio.getFromPhoneNumber());
            formData.add("Body", String.format("Your Miloo verification code is: %s. Valid for %d minutes.",
                    otpCode, properties.getTtlMinutes()));

            RestClient restClient = restClientBuilder.build();
            restClient.post()
                    .uri(url)
                    .header("Authorization", basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[Twilio] Successfully sent SMS OTP to '{}'", destination);
        } catch (Exception e) {
            log.error("[Twilio] Failed to send SMS OTP to '{}': {}", destination, e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to send SMS OTP via Twilio: " + e.getMessage());
        }
    }

    @Override
    public OtpProvider getProviderType() {
        return OtpProvider.TWILIO;
    }

    @Override
    public OtpChannel getChannel() {
        return OtpChannel.SMS;
    }
}
