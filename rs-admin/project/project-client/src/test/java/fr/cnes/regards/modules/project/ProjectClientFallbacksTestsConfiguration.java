/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.client.core.ClientRequestInterceptor;

/**
 *
 * Class ProjectClientFallbacksTestsConfiguration
 *
 * Test configuration class
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ComponentScan
@EnableFeignClients(defaultConfiguration = { ClientRequestInterceptor.class })
@EnableAutoConfiguration
public class ProjectClientFallbacksTestsConfiguration {

}
