package com.miloo.common.otp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry and factory for dynamically looking up and selecting OTP sender providers at runtime.
 * Mirrors the architecture of StorageServiceFactory.
 */
@Component
public class OtpSenderFactory {

    private final OtpSenderService twilioOtpSenderService;
    private final OtpSenderService msg91OtpSenderService;
    private final OtpSenderService smtpEmailOtpSenderService;
    private final OtpProperties properties;
    private final Map<OtpProvider, OtpSenderService> providerMap = new HashMap<>();

    public OtpSenderFactory(
            @Qualifier("twilioOtpSenderService") OtpSenderService twilioOtpSenderService,
            @Qualifier("msg91OtpSenderService") OtpSenderService msg91OtpSenderService,
            @Qualifier("smtpEmailOtpSenderService") OtpSenderService smtpEmailOtpSenderService,
            OtpProperties properties
    ) {
        this.twilioOtpSenderService = twilioOtpSenderService;
        this.msg91OtpSenderService = msg91OtpSenderService;
        this.smtpEmailOtpSenderService = smtpEmailOtpSenderService;
        this.properties = properties;

        providerMap.put(OtpProvider.TWILIO, twilioOtpSenderService);
        providerMap.put(OtpProvider.MSG91, msg91OtpSenderService);
        providerMap.put(OtpProvider.SMTP, smtpEmailOtpSenderService);
    }

    /**
     * Resolves the active SMS sender provider configured in application.yml (twilio or msg91).
     */
    public OtpSenderService getActiveSmsSender() {
        return getSender(properties.getSmsProviderEnum());
    }

    /**
     * Resolves the active Email sender provider configured in application.yml (smtp).
     */
    public OtpSenderService getActiveEmailSender() {
        return getSender(properties.getEmailProviderEnum());
    }

    /**
     * Retrieves the sender service matching the specified provider enum.
     */
    public OtpSenderService getSender(OtpProvider provider) {
        if (provider == null) {
            return twilioOtpSenderService;
        }
        OtpSenderService service = providerMap.get(provider);
        return service != null ? service : twilioOtpSenderService;
    }

    /**
     * Retrieves the sender service matching the specified provider name string.
     */
    public OtpSenderService getSender(String providerName) {
        return getSender(OtpProvider.fromString(providerName));
    }

    /**
     * Automatically resolves the appropriate sender for the destination:
     * - Email addresses route to the configured active email sender (SMTP).
     * - Phone numbers route to the configured active SMS sender (Twilio or MSG91).
     */
    public OtpSenderService getSenderForDestination(String destination) {
        OtpChannel channel = OtpChannel.fromDestination(destination);
        if (channel == OtpChannel.EMAIL) {
            return getActiveEmailSender();
        }
        return getActiveSmsSender();
    }

    /**
     * Resolves the appropriate sender with an optional explicit provider override.
     */
    public OtpSenderService getSenderForDestination(String destination, String providerOverride) {
        if (providerOverride != null && !providerOverride.trim().isEmpty()) {
            return getSender(providerOverride);
        }
        return getSenderForDestination(destination);
    }
}
