package com.miloo.common.otp;

import com.miloo.common.otp.impl.Msg91OtpSenderService;
import com.miloo.common.otp.impl.SmtpEmailOtpSenderService;
import com.miloo.common.otp.impl.TwilioOtpSenderService;
import com.miloo.config.OtpConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        OtpConfig.class,
        OtpSenderFactory.class,
        OtpSenderRouter.class,
        TwilioOtpSenderService.class,
        Msg91OtpSenderService.class,
        SmtpEmailOtpSenderService.class
})
@TestPropertySource(properties = {
        "app.otp.sms-provider=msg91",
        "app.otp.email-provider=smtp",
        "app.otp.mock-enabled=true",
        "app.otp.ttl-minutes=5"
})
class OtpContextTest {

    @Autowired
    private OtpSenderService primaryOtpSenderService;

    @Autowired
    @Qualifier("twilioOtpSenderService")
    private OtpSenderService twilioOtpSenderService;

    @Autowired
    @Qualifier("msg91OtpSenderService")
    private OtpSenderService msg91OtpSenderService;

    @Autowired
    @Qualifier("smtpEmailOtpSenderService")
    private OtpSenderService smtpEmailOtpSenderService;

    @Autowired
    private OtpSenderFactory factory;

    @Autowired
    private OtpProperties properties;

    @Test
    @DisplayName("Spring Context loads all OTP sender provider beans and dynamically binds active providers")
    void testBeansInjectedAndPropertiesBound() {
        assertNotNull(primaryOtpSenderService);
        assertNotNull(twilioOtpSenderService);
        assertNotNull(msg91OtpSenderService);
        assertNotNull(smtpEmailOtpSenderService);
        assertNotNull(factory);
        assertNotNull(properties);

        assertEquals("msg91", properties.getSmsProvider());
        assertEquals(OtpProvider.MSG91, factory.getActiveSmsSender().getProviderType());
        assertEquals(OtpProvider.SMTP, factory.getActiveEmailSender().getProviderType());
        assertEquals(OtpProvider.MSG91, primaryOtpSenderService.getProviderType());
    }
}
