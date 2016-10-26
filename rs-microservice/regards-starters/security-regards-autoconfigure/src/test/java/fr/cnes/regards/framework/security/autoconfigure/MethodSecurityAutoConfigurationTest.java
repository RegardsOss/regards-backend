/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.controller.SecurityResourcesController;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.endpoint.IPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.filter.JWTAuthenticationProvider;

/**
 * @author msordi
 *
 */
public class MethodSecurityAutoConfigurationTest {

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
        this.context.register(MethodSecurityAutoConfiguration.class, MethodAuthorizationServiceAutoConfiguration.class,
                              WebSecurityAutoConfiguration.class);
        this.context.refresh();
        Assertions.assertThat(this.context.getBean(IAuthoritiesProvider.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(ITenantResolver.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(MethodAuthorizationService.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(IPluginResourceManager.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(SecurityResourcesController.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(JWTAuthenticationProvider.class)).isNotNull();

    }

}
