package fr.cnes.regards.modules.storage.service.forTest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.search.client.ISearchClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class MockedFeignClientConf {

    @Bean
    public INotificationClient notificationClient() {
        return Mockito.mock(INotificationClient.class);
    }

    @Bean
    public ISearchClient searchClient() {
        return Mockito.mock(ISearchClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

}
