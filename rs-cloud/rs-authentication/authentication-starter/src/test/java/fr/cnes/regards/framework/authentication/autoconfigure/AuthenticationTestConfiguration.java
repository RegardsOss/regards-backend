/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.autoconfigure;

import java.util.ArrayList;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.dao.stubs.PluginConfigurationRepositoryStub;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

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
@PropertySource("classpath:test.properties")
public class AuthenticationTestConfiguration {

    public final static String PROJECT_TEST_NAME = "PROJECT";

    public final static String VALID_PASSWORD = "valid";

    public final static String INVALID_PASSWORD = "invalid";

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
        testUser.setStatus(UserStatus.ACCESS_GRANTED);

        final ResponseEntity<Resource<ProjectUser>> response = new ResponseEntity<Resource<ProjectUser>>(
                new Resource<ProjectUser>(testUser, new ArrayList<>()), HttpStatus.OK);
        Mockito.when(client.retrieveProjectUser(Mockito.anyString())).thenReturn(response);

        return client;
    }

    @Bean
    @Primary
    IProjectsClient projectsClient() {
        final IProjectsClient client = Mockito.mock(IProjectsClient.class);

        final ResponseEntity<Resource<Project>> response = new ResponseEntity<Resource<Project>>(
                new Resource<Project>(new Project("", "", true, PROJECT_TEST_NAME)), HttpStatus.OK);
        Mockito.when(client.retrieveProject(Mockito.anyString())).thenReturn(response);
        return client;
    }

    @Bean
    IAccountsClient accountsClient() {
        final IAccountsClient mock = Mockito.mock(IAccountsClient.class);
        final Account account = new Account("email@test.fr", "name", "lastname", "password");
        account.setStatus(AccountStatus.ACTIVE);
        final Resource<Account> resource = HateoasUtils.wrap(account);
        final ResponseEntity<Resource<Account>> response = ResponseEntity.ok(resource);

        Mockito.when(mock.retrieveAccounByEmail(Mockito.any())).thenReturn(response);

        // Password validation depends on password only for testing
        Mockito.when(mock.validatePassword(Mockito.anyString(), Mockito.eq(INVALID_PASSWORD)))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));
        Mockito.when(mock.validatePassword(Mockito.anyString(), Mockito.eq(VALID_PASSWORD)))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));
        return mock;
    }

}
