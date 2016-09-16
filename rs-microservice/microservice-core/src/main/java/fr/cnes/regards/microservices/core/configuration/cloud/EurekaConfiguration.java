/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.cloud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true")
@Configuration
@EnableEurekaClient
@EnableDiscoveryClient
public class EurekaConfiguration {

}
