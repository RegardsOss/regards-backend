/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 *
 * Class RemoteFeignClientAutoConfiguration
 *
 * Auto-configuration to enable feign clients. This configuration is alone to allow tests to exlude it.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@ConditionalOnProperty(name = "regards.cloud.enabled", matchIfMissing = true)
@EnableFeignClients(clients = { IProjectsClient.class, IProjectUsersClient.class, IAccountsClient.class })
public class RemoteFeignClientAutoConfiguration {

}
