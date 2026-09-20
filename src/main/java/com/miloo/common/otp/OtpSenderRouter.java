package com.miloo.common.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Primary routing facade for OtpSenderService.
 * Automatically delegates all calls to the active provider selected in application.yml
 * based on whether the destination is phone or email.
 * Mirrors the architecture of StorageServiceRouter.
 */
@Slf4j
@Primary
@Service("otpSenderService")
@RequiredArgsConstructor
public class OtpSenderRouter implements OtpSenderService {

    private final OtpSenderFactory factory;

    @Override
    public void sendOtp(String destination, String otpCode) {
        OtpSenderService delegate = factory.getSenderForDestination(destination);
        delegate.sendOtp(destination, otpCode);
    }

    @Override
    public OtpProvider getProviderType() {
        return factory.getActiveSmsSender().getProviderType();
    }

    @Override
    public OtpChannel getChannel() {
        return OtpChannel.SMS;
    }
}
