package com.miloo.common.otp;

import com.miloo.common.otp.impl.Msg91OtpSenderService;
import com.miloo.common.otp.impl.SmtpEmailOtpSenderService;
import com.miloo.common.otp.impl.TwilioOtpSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OtpSenderServiceTest {

    private OtpProperties properties;
    private TwilioOtpSenderService twilioService;
    private Msg91OtpSenderService msg91Service;
    private SmtpEmailOtpSenderService smtpService;
    private OtpSenderFactory factory;
    private OtpSenderRouter router;

    @BeforeEach
    void setUp() {
        properties = new OtpProperties();
        properties.setMockEnabled(true);
        properties.setTtlMinutes(5);

        RestClient.Builder restClientBuilder = RestClient.builder();
        JavaMailSender mailSender = mock(JavaMailSender.class);

        twilioService = new TwilioOtpSenderService(properties, restClientBuilder);
        msg91Service = new Msg91OtpSenderService(properties, restClientBuilder);
        smtpService = new SmtpEmailOtpSenderService(properties, mailSender);

        factory = new OtpSenderFactory(twilioService, msg91Service, smtpService, properties);
        router = new OtpSenderRouter(factory);
    }

    @Test
    @DisplayName("TwilioOtpSenderService correctly identifies provider type, channel, and dispatches in mock mode")
    void testTwilioSender() {
        assertEquals(OtpProvider.TWILIO, twilioService.getProviderType());
        assertEquals(OtpChannel.SMS, twilioService.getChannel());
        assertDoesNotThrow(() -> twilioService.sendOtp("+15005550006", "123456"));
    }

    @Test
    @DisplayName("Msg91OtpSenderService correctly identifies provider type, channel, and dispatches in mock mode")
    void testMsg91Sender() {
        assertEquals(OtpProvider.MSG91, msg91Service.getProviderType());
        assertEquals(OtpChannel.SMS, msg91Service.getChannel());
        assertDoesNotThrow(() -> msg91Service.sendOtp("+919876543210", "654321"));
    }

    @Test
    @DisplayName("SmtpEmailOtpSenderService correctly identifies provider type, channel, and dispatches in mock mode")
    void testSmtpSender() {
        assertEquals(OtpProvider.SMTP, smtpService.getProviderType());
        assertEquals(OtpChannel.EMAIL, smtpService.getChannel());
        assertDoesNotThrow(() -> smtpService.sendOtp("test@example.com", "888999"));
    }

    @Test
    @DisplayName("OtpSenderFactory resolves sender by Enum and by String")
    void testFactoryResolvesByEnumAndString() {
        assertSame(twilioService, factory.getSender(OtpProvider.TWILIO));
        assertSame(msg91Service, factory.getSender(OtpProvider.MSG91));
        assertSame(smtpService, factory.getSender(OtpProvider.SMTP));

        assertSame(twilioService, factory.getSender("twilio"));
        assertSame(msg91Service, factory.getSender("msg91"));
        assertSame(smtpService, factory.getSender("smtp"));
    }

    @Test
    @DisplayName("OtpSenderFactory dynamically switches active SMS sender based on application properties")
    void testFactoryDynamicSmsProviderSwitch() {
        properties.setSmsProvider("twilio");
        assertSame(twilioService, factory.getActiveSmsSender());

        properties.setSmsProvider("msg91");
        assertSame(msg91Service, factory.getActiveSmsSender());
    }

    @Test
    @DisplayName("OtpSenderFactory routes destination automatically based on email vs phone format")
    void testFactoryRouteByDestination() {
        properties.setSmsProvider("twilio");

        // Phone number routes to active SMS provider (Twilio)
        assertSame(twilioService, factory.getSenderForDestination("+1234567890"));

        // Switch active SMS provider to MSG91
        properties.setSmsProvider("msg91");
        assertSame(msg91Service, factory.getSenderForDestination("+919876543210"));

        // Email address routes to active Email provider (SMTP)
        assertSame(smtpService, factory.getSenderForDestination("user@example.com"));
    }

    @Test
    @DisplayName("OtpSenderFactory respects explicit provider override")
    void testFactoryProviderOverride() {
        properties.setSmsProvider("twilio");

        // Even though active is Twilio, explicit override specifies MSG91
        assertSame(msg91Service, factory.getSenderForDestination("+1234567890", "msg91"));

        // Even though phone number, explicit override can specify SMTP if requested
        assertSame(smtpService, factory.getSenderForDestination("+1234567890", "smtp"));
    }

    @Test
    @DisplayName("OtpSenderRouter dispatches without error")
    void testRouterSendOtp() {
        properties.setSmsProvider("twilio");
        assertDoesNotThrow(() -> router.sendOtp("+1234567890", "112233"));
        assertDoesNotThrow(() -> router.sendOtp("hello@miloo.app", "445566"));
    }
}
