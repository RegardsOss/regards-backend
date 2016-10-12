/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.cloud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Class EurekaConfiguration
 *
 * Spring configuration to enable the Microservice to user Eureka registry for register himself and communicate with
 * others microservices.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ConditionalOnProperty(name = "regards.cloud.enabled", havingValue = "true")
@Configuration
@EnableEurekaClient
@EnableDiscoveryClient
public class EurekaConfiguration {

}
