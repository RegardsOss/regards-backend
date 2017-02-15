/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

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
        return Mockito.mock(JavaMailSender.class);
    }
}
