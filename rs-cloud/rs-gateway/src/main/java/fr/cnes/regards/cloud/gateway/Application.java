/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ConfigurableApplicationContext;

import fr.cnes.regards.client.core.ClientRequestInterceptor;

/**
 *
 * Class GatewayApplication
 *
 * Spring boot starter class for Regards Gateway component
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@SpringBootApplication(scanBasePackages = "fr.cnes.regards")
@EnableFeignClients(basePackages = "fr.cnes.regards", defaultConfiguration = { ClientRequestInterceptor.class })
@EnableZuulProxy
public class Application {

    /**
     *
     * Starter method
     *
     * @param pArgs
     *            params
     * @since 1.0-SNAPSHOT
     */
    public static void main(String[] pArgs) {
        final ConfigurableApplicationContext context = SpringApplication.run(Application.class, pArgs);
        context.close();
    }
}
