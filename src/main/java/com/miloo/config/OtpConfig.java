package com.miloo.config;

import com.miloo.common.otp.OtpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;

import java.util.Properties;

@Slf4j
@Configuration
@EnableConfigurationProperties(OtpProperties.class)
@RequiredArgsConstructor
public class OtpConfig {

    private final OtpProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        OtpProperties.SmtpProperties smtp = properties.getSmtp();

        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setUsername(smtp.getUsername());
        mailSender.setPassword(smtp.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtp.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtp.isStarttls()));
        props.put("mail.debug", "false");

        return mailSender;
    }
}
