/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

import fr.cnes.regards.client.core.ClientRequestInterceptor;
import fr.cnes.regards.cloud.gateway.filters.ProxyLogFilter;
import fr.cnes.regards.modules.accessRights.client.IAccountsClient;

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
@EnableFeignClients(clients = IAccountsClient.class, defaultConfiguration = { ClientRequestInterceptor.class })
@EnableZuulProxy
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

}
