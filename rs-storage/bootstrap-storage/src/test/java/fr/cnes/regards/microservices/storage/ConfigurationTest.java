package fr.cnes.regards.microservices.storage;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import fr.cnes.regards.modules.project.client.rest.ITenantConnectionClient;

@TestConfiguration
public class ConfigurationTest {

    @Bean
    public ITenantConnectionClient client() {
        return Mockito.mock(ITenantConnectionClient.class);
    }

}
