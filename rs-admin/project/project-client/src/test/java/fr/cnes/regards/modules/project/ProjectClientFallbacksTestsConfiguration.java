/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

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
@EnableFeignClients(clients = { IProjectsClient.class, IProjectConnectionClient.class })
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class ProjectClientFallbacksTestsConfiguration {

}
