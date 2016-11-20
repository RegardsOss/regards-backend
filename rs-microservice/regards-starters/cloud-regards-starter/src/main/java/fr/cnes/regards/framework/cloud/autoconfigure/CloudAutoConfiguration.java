/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.cloud.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "regards.cloud", name = "enabled", matchIfMissing = true)
@EnableEurekaClient
@EnableDiscoveryClient
public class CloudAutoConfiguration {

}
