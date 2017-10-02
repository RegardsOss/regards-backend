/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.hateoas.IResourceService;

@Configuration
public class TestConfig {

    @Bean
    public IResourceService resourceService() {
        return Mockito.mock(IResourceService.class);
    }

}
