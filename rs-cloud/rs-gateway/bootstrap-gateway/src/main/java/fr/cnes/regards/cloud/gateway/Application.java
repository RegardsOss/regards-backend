/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.SimpleAuthentication;
import fr.cnes.regards.cloud.gateway.filters.ProxyLogFilter;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;

/**
 *
 * Class GatewayApplication
 *
 * Spring boot starter class for Regards Gateway component
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@SpringBootApplication
@EnableZuulProxy
@EnableAutoConfiguration
public class Application { // NOSONAR

    /**
     *
     * Starter method
     *
     * @param pArgs
     *            params
     * @since 1.0-SNAPSHOT
     */
    public static void main(final String[] pArgs) {
        SpringApplication.run(Application.class, pArgs); // NOSONAR
    }

    /**
     *
     * Create zuul proxy filter
     *
     * @return ProxyLogFilter
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ProxyLogFilter proxyLogFilter() {
        return new ProxyLogFilter();
    }

    @Bean
    public IAuthenticationPlugin plugin(@Value("${spring.application.name") String pMicorserviceName,
            IAccountsClient pAccountsClient, IProjectUsersClient pProjectUsersClient, JWTService pJwtService) {
        return new SimpleAuthentication(pMicorserviceName, pAccountsClient, pProjectUsersClient, pJwtService);
    }

}
