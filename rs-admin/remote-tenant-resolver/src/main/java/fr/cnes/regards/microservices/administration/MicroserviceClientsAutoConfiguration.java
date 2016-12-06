/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
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
@Configuration
@AutoConfigureBefore(MicroserviceAutoConfiguration.class)
@ConditionalOnProperty(name = "regards.cloud.enabled", matchIfMissing = true)
@EnableFeignClients(
        clients = { IProjectsClient.class, IProjectConnectionClient.class, IResourcesClient.class, IRolesClient.class })
public class MicroserviceClientsAutoConfiguration {
}
