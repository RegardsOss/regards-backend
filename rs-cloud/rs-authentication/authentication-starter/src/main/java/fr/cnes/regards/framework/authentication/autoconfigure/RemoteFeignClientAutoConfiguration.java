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
package fr.cnes.regards.framework.authentication.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
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
