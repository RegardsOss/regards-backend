/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import java.util.ArrayList;

import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.cloud.gateway.authentication.configuration.RemoteFeignClientAutoConfiguration;
import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.stub.AuthenticationPluginStub;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.dao.stubs.PluginConfigurationRepositoryStub;

/**
 *
 * Class AuthenticationTestConfiguration
 *
 * Test configuration class
 *
 * @author Sébastien Binda
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.cloud.gateway", "fr.cnes.regards.modules" })
@PropertySource("classpath:test.properties")
@EnableAutoConfiguration(exclude = { RemoteFeignClientAutoConfiguration.class, DataSourceAutoConfiguration.class })
public class AuthenticationTestConfiguration {

    @Bean
    IAuthenticationPlugin defaultAuthenticationPlugin() {
        return new AuthenticationPluginStub();
    }

    @Bean
    IPluginConfigurationRepository pluginConfigurationRepo() {
        return new PluginConfigurationRepositoryStub();
    }

    @Bean
    @Primary
    IProjectUsersClient client() {
        final IProjectUsersClient client = Mockito.mock(IProjectUsersClient.class);

        final Role testRole = new Role();
        testRole.setName("TEST");
        final ProjectUser testUser = new ProjectUser();
        testUser.setEmail("test@regards.fr");
        testUser.setRole(testRole);

        final ResponseEntity<Resource<ProjectUser>> response = new ResponseEntity<Resource<ProjectUser>>(
                new Resource<ProjectUser>(testUser, new ArrayList<>()), HttpStatus.OK);
        try {
            Mockito.when(client.retrieveProjectUser(Mockito.anyString())).thenReturn(response);
        } catch (final EntityNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        return client;
    }

}
