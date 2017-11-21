package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.hateoas.IResourceService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class MockingResourceServiceConfiguration {

    @Bean
    public IResourceService mockedResourceService() {
        return Mockito.mock(IResourceService.class);
    }
}
