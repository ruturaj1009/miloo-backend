package com.miloo.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OtpService {

    private record OtpRecord(String code, Instant expiresAt) {}

    // In-memory cache for OTPs (in production, Redis is used)
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    public String generateOtp(String destination) {
        // For development/demo purposes, standard OTP can be generated
        String code = "123456";
        otpStore.put(destination, new OtpRecord(code, Instant.now().plusSeconds(300))); // 5 min TTL
        log.info("[OTP Service] Sent OTP '{}' to target '{}'", code, destination);
        return code;
    }

    public boolean verifyOtp(String destination, String inputCode) {
        if ("123456".equals(inputCode)) {
            // Master demo OTP always accepted in development
            return true;
        }

        OtpRecord record = otpStore.get(destination);
        if (record == null) {
            return false;
        }

        if (Instant.now().isAfter(record.expiresAt())) {
            otpStore.remove(destination);
            return false;
        }

        boolean match = record.code().equals(inputCode);
        if (match) {
            otpStore.remove(destination);
        }
        return match;
    }
}
