/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import javax.mail.internet.MimeMessage;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Mock {@link JavaMailSender}
 *
 * @author Marc Sordi
 *
 */
@Configuration
public class EmailConfiguration {

    @Bean
    public JavaMailSender mockSender() {
        final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
        return mailSender;
    }
}
