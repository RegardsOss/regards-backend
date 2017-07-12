/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
