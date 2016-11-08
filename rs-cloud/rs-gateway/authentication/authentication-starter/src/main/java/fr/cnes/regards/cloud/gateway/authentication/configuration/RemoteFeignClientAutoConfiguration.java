/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import org.springframework.cloud.netflix.feign.EnableFeignClients;

import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;

/**
 *
 * Class RemoteFeignClientAutoConfiguration
 *
 * Auto-configuration to enable feign clients. This configuration is alone to allow tests to exlude it.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@EnableFeignClients(clients = { IProjectUsersClient.class, IAccountsClient.class })
public class RemoteFeignClientAutoConfiguration {

}
