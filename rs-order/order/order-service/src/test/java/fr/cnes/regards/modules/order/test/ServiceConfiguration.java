package fr.cnes.regards.modules.order.test;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order" })
@EnableAutoConfiguration
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
public class ServiceConfiguration {
    @Bean
    public ISubscriber mockSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    public IPublisher mockPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    @Bean
    public IInstanceSubscriber mockInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }

    @Bean
    public IInstancePublisher mockInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }

    @Bean
    public ICatalogClient mockCatalogClient() {
        return new CatalogClientMock();
    }
}
