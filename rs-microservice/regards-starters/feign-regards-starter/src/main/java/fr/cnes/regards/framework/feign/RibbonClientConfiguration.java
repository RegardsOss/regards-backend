/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

/**
 *
 * Class RibbonClientConfiguration
 *
 * Configuration class for Ribbon clients.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class RibbonClientConfiguration {

    /**
     *
     * Ribbon Properties configuration
     *
     * @return IClientConfig
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IClientConfig config() {
        final IClientConfig config = new DefaultClientConfigImpl();
        config.set(CommonClientConfigKey.ReadTimeout, 5000);
        config.set(CommonClientConfigKey.ConnectTimeout, 5000);
        return config;
    }

    /**
     *
     * Configure ribbon load balancer
     *
     * @return ILoadBalancer
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ILoadBalancer loadBalancerConfig() {
        return new ZoneAwareLoadBalancer<>();
    }

}
