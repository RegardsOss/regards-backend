/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.emails.client.IEmailClient;

/**
 * @author Marc Sordi
 *
 */
@Configuration
public class FeignClientConfiguration {

    @Bean
    public IEmailClient emailClient() {
        return Mockito.mock(IEmailClient.class);
    }
}
