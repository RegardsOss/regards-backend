/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.LocalTenantResolver;
import fr.cnes.regards.framework.security.endpoint.DefaultAuthorityProvider;
import fr.cnes.regards.framework.security.endpoint.DefaultPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

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

    @Test
    public void testMethodConfiguration() {
        this.context = new AnnotationConfigWebApplicationContext();
        this.context.setServletContext(new MockServletContext());
        this.context.register(MethodSecurityAutoConfiguration.class, MethodAuthorizationService.class,
                              DefaultPluginResourceManager.class, DefaultAuthorityProvider.class,
                              LocalTenantResolver.class, JWTService.class);
        this.context.refresh();
        Assertions.assertThat(this.context.getBean(MethodAuthorizationService.class)).isNotNull();
    }

}
