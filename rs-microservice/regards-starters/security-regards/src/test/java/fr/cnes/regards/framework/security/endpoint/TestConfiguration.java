/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Class TestConfiguration
 *
 * Configuration for spring injections
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@PropertySource("classpath:auth-provider-test.properties")
public class TestConfiguration {

    /**
     * Valid Tenant name for tests
     */
    public static final String TENANT_1 = "tenant-1";

    /**
     * Valid Tenant name for tests
     */
    public static final String TENANT_2 = "tenant-2";

    @Bean
    IAuthoritiesProvider provider() {
        return new DefaultAuthorityProvider();
    }

    @Bean
    IPluginResourceManager pluginManager() {
        return new DefaultPluginResourceManager();
    }

    @Bean
    MethodAuthorizationService methodAuthService() {
        return new MethodAuthorizationService();
    }

    @Bean
    JWTService jwtService() {
        return new JWTService();
    }

    @Bean
    IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    @Bean
    ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    ITenantResolver tenantResolver() {
        return new ITenantResolver() {

            @Override
            public Set<String> getAllTenants() {
                final Set<String> tenants = new HashSet<>();
                tenants.add(TENANT_1);
                tenants.add(TENANT_2);
                return tenants;
            }
        };
    }

}
