package com.miloo.modules.auth.service;

import com.miloo.common.exception.ApiException;
import com.miloo.common.otp.*;
import com.miloo.modules.auth.entity.OtpEntity;
import com.miloo.modules.auth.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final OtpSenderFactory otpSenderFactory;
    private final OtpProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 6-digit OTP, stores it in PostgreSQL with a 5-minute TTL, and sends it via
     * the active provider configured for the destination channel.
     *
     * @param destination Target phone number or email address
     * @return Generated 6-digit OTP code
     */
    @Transactional
    public String generateOtp(String destination) {
        return generateOtp(destination, null);
    }

    /**
     * Generates a 6-digit OTP, stores it in PostgreSQL with a 5-minute TTL, and sends it via
     * the specified provider override or active default provider.
     *
     * @param destination Target phone number or email address
     * @param providerOverride Optional provider override (e.g. "twilio", "msg91", "smtp")
     * @return Generated 6-digit OTP code
     */
    @Transactional
    public String generateOtp(String destination, String providerOverride) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Destination (phone number or email) is required");
        }
        String cleanDestination = destination.trim();

        // 1. Invalidate any existing PENDING OTPs for this destination
        otpRepository.updateStatusByDestinationAndStatus(cleanDestination, OtpStatus.PENDING, OtpStatus.EXPIRED);

        // 2. Generate secure 6-digit OTP code
        int randomCode = secureRandom.nextInt(900000) + 100000;
        String code = String.valueOf(randomCode);

        // 3. Resolve channel and appropriate sender
        OtpChannel channel = OtpChannel.fromDestination(cleanDestination);
        OtpSenderService sender = otpSenderFactory.getSenderForDestination(cleanDestination, providerOverride);

        // 4. Calculate 5-minute TTL expiration
        int ttlMinutes = properties.getTtlMinutes() > 0 ? properties.getTtlMinutes() : 5;
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);

        // 5. Persist OTP record in PostgreSQL database
        OtpEntity otpEntity = OtpEntity.builder()
                .destination(cleanDestination)
                .otpCode(code)
                .channel(channel)
                .provider(sender.getProviderType())
                .status(OtpStatus.PENDING)
                .attempts(0)
                .expiresAt(expiresAt)
                .build();
        otpRepository.save(otpEntity);

        log.info("[OTP Service] Saved OTP to DB for destination '{}' via provider '{}' (Expires in {} mins)",
                cleanDestination, sender.getProviderType(), ttlMinutes);

        // 6. Dispatch message via chosen provider
        sender.sendOtp(cleanDestination, code);

        return code;
    }

    /**
     * Verifies the user's OTP code against PostgreSQL records, checking expiration (5 min TTL)
     * and max verification attempts.
     *
     * @param destination Target phone number or email address
     * @param inputCode OTP verification code entered by user
     * @return true if OTP is valid, false otherwise
     */
    @Transactional
    public boolean verifyOtp(String destination, String inputCode) {
        if (destination == null || inputCode == null) {
            return false;
        }
        String cleanDestination = destination.trim();
        String cleanCode = inputCode.trim();

        Optional<OtpEntity> otpOpt = otpRepository.findTopByDestinationAndStatusOrderByCreatedAtDesc(
                cleanDestination, OtpStatus.PENDING);

        if (otpOpt.isEmpty()) {
            // Master demo OTP accepted if mockEnabled is active and no active DB OTP found
            if (properties.isMockEnabled() && "123456".equals(cleanCode)) {
                log.info("[OTP Service] Mock mode accepted master demo OTP '123456' for destination '{}'", cleanDestination);
                return true;
            }
            log.warn("[OTP Service] No active pending OTP found in DB for '{}'", cleanDestination);
            return false;
        }

        OtpEntity otp = otpOpt.get();

        // Check if OTP has expired (5-minute TTL)
        if (Instant.now().isAfter(otp.getExpiresAt())) {
            otp.setStatus(OtpStatus.EXPIRED);
            otpRepository.save(otp);
            log.warn("[OTP Service] OTP for '{}' has expired at {}", cleanDestination, otp.getExpiresAt());
            return false;
        }

        // Check if maximum attempts exceeded
        if (otp.getAttempts() >= properties.getMaxAttempts()) {
            otp.setStatus(OtpStatus.FAILED);
            otpRepository.save(otp);
            log.warn("[OTP Service] Max attempts exceeded ({}) for '{}'", properties.getMaxAttempts(), cleanDestination);
            return false;
        }

        boolean isMatch = otp.getOtpCode().equals(cleanCode) ||
                (properties.isMockEnabled() && "123456".equals(cleanCode));

        if (isMatch) {
            otp.setStatus(OtpStatus.VERIFIED);
            otpRepository.save(otp);
            log.info("[OTP Service] OTP successfully verified for '{}'", cleanDestination);
            return true;
        } else {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= properties.getMaxAttempts()) {
                otp.setStatus(OtpStatus.FAILED);
            }
            otpRepository.save(otp);
            log.warn("[OTP Service] Incorrect OTP code for '{}' (Attempt {}/{})",
                    cleanDestination, otp.getAttempts(), properties.getMaxAttempts());
            return false;
        }
    }
}
