package fr.cnes.regards.modules.acquisition.rest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.notification.client.INotificationClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class NotificationClientMockConfiguration {

    @Bean
    public INotificationClient notificationClient() {return Mockito.mock(INotificationClient.class);}
}
