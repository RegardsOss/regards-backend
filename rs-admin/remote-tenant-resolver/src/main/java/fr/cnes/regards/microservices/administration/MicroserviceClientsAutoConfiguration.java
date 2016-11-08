/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.cloud.netflix.feign.EnableFeignClients;

import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 *
 * Class MicroserviceClientsAutoConfiguration
 *
 * Auto-configuration to enable feign clients needed by this auto-configure module. This configuration is alone in order
 * to allow tests to exclude Feign clients configuration.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT.
 */
@EnableFeignClients(clients = { IProjectsClient.class, IProjectConnectionClient.class })
public class MicroserviceClientsAutoConfiguration {

}
