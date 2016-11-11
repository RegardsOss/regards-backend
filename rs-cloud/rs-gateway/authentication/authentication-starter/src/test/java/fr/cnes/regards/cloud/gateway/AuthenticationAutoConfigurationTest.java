/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import fr.cnes.regards.cloud.gateway.authentication.configuration.Oauth2AutoConfiguration;
import fr.cnes.regards.cloud.gateway.authentication.configuration.Oauth2WebAutoConfiguration;
import fr.cnes.regards.cloud.gateway.authentication.configuration.RemoteFeignClientAutoConfiguration;
import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.framework.authentication.Oauth2AuthenticationManager;
import fr.cnes.regards.framework.authentication.Oauth2AuthorizationServerConfigurer;
import fr.cnes.regards.framework.authentication.Oauth2EndpointsConfiguration;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.fallback.AccountsFallback;
import fr.cnes.regards.modules.accessrights.fallback.ProjectUsersFallback;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.client.rest.fallback.ProjectsFallback;

/**
 * Class AuthenticationAutoConfigurationTest
 *
 * Test class for Authentication starter auto configuration class
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 *
 */
public class AuthenticationAutoConfigurationTest {

    /**
     * Web application context
     */
    private AnnotationConfigWebApplicationContext context;

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    /**
     *
     * Check for security autoconfigure
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testMethodConfiguration() {
        this.context = new AnnotationConfigWebApplicationContext();
        this.context.setServletContext(new MockServletContext());
        this.context.refresh();
        this.context.register(ServerPropertiesAutoConfiguration.class, SecurityAutoConfiguration.class,
                              FeignAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class,
                              RibbonAutoConfiguration.class, RemoteFeignClientAutoConfiguration.class,
                              Oauth2WebAutoConfiguration.class, Oauth2AutoConfiguration.class, JWTService.class,
                              AccountsFallback.class, ProjectsFallback.class, ProjectUsersFallback.class);
        this.context.refresh();
        Assertions.assertThat(this.context.getBean(IProjectsClient.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(IProjectUsersClient.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(IAccountsClient.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(IAuthenticationPlugin.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(Oauth2AuthenticationManager.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(Oauth2AuthorizationServerConfigurer.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(Oauth2EndpointsConfiguration.class)).isNotNull();
    }

}
