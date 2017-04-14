/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.authentication.rest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 * @author Marc Sordi
 *
 */
@Configuration
public class FeignMockConfiguration {

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }
}
