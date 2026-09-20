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
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service("msg91OtpSenderService")
@RequiredArgsConstructor
public class Msg91OtpSenderService implements OtpSenderService {

    private final OtpProperties properties;
    private final RestClient.Builder restClientBuilder;

    private boolean isDummy(String val) {
        return val == null || val.isBlank() || val.contains("dummy");
    }

    @Override
    public void sendOtp(String destination, String otpCode) {
        OtpProperties.Msg91Properties msg91 = properties.getMsg91();

        if (properties.isMockEnabled() || isDummy(msg91.getAuthKey())) {
            log.info("[MSG91 Mock] SMS OTP '{}' dispatched to destination '{}' (SenderId: {}, Template: {})",
                    otpCode, destination, msg91.getSenderId(), msg91.getTemplateId());
            return;
        }

        try {
            // MSG91 OTP API v5: POST https://control.msg91.com/api/v5/otp
            String cleanMobile = destination.replaceAll("[^0-9]", "");
            String url = "https://control.msg91.com/api/v5/otp";

            RestClient restClient = restClientBuilder.build();
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("control.msg91.com")
                            .path("/api/v5/otp")
                            .queryParam("template_id", msg91.getTemplateId())
                            .queryParam("mobile", cleanMobile)
                            .queryParam("authkey", msg91.getAuthKey())
                            .queryParam("otp", otpCode)
                            .queryParam("otp_expiry", properties.getTtlMinutes())
                            .build())
                    .header("authkey", msg91.getAuthKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[MSG91] Successfully sent SMS OTP to '{}'", destination);
        } catch (Exception e) {
            log.error("[MSG91] Failed to send SMS OTP to '{}': {}", destination, e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to send SMS OTP via MSG91: " + e.getMessage());
        }
    }

    @Override
    public OtpProvider getProviderType() {
        return OtpProvider.MSG91;
    }

    @Override
    public OtpChannel getChannel() {
        return OtpChannel.SMS;
    }
}
