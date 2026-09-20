package com.miloo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /**
     * Base URL for account activation links sent in welcome emails.
     * Configurable in application.yml via app.auth.activation-base-url
     */
    private String activationBaseUrl = "http://localhost:8080/api/v1/auth/activate";

    /**
     * Expiration TTL in hours for account activation tokens (default 24 hours).
     * Configurable in application.yml via app.auth.activation-token-ttl-hours
     */
    private int activationTokenTtlHours = 24;

    /**
     * When true, welcome and activation emails are simulated and logged to the console
     * without attempting actual SMTP transmission.
     * Configurable in application.yml via app.auth.mock-email-enabled
     */
    private boolean mockEmailEnabled = false;
}
