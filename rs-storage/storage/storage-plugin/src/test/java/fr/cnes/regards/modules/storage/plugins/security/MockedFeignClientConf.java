package fr.cnes.regards.modules.storage.plugins.security;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class MockedFeignClientConf {

    @Bean
    public ICatalogClient catalogClient() {
        return Mockito.mock(ICatalogClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

}
