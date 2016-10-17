/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.IMethodAuthorizationService;
import fr.cnes.regards.framework.security.autoconfigure.endpoint.DefaultMethodAuthorizationService;

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
        this.context.register(MethodSecurityAutoConfiguration.class, DefaultMethodAuthorizationService.class);
        this.context.refresh();
        Assertions.assertThat(this.context.getBean(IMethodAuthorizationService.class)).isNotNull();
    }

}
