package fr.cnes.regards.modules.storage.plugins.security;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.search.client.ISearchClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class MockedFeignClientConf {

    @Bean
    public ISearchClient searchClient() {
        return Mockito.mock(ISearchClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

}
