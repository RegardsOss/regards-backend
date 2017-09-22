package fr.cnes.regards.modules.order.test;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order.service" })
@EnableAutoConfiguration
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
public class ServiceConfiguration {

    @Bean
    public ICatalogClient mockCatalogClient() {
        return new CatalogClientMock();
    }

    @Bean
    public IAipClient mockAipClient() {
        return Mockito.mock(IAipClient.class);
    }

    @Bean
    public IAuthenticationResolver mockAuthResolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }

}
