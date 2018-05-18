/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
@AutoConfigureBefore(RemoteTenantAutoConfiguration.class)
@ConditionalOnProperty(name = "regards.cloud.enabled", matchIfMissing = true)
@EnableFeignClients(
        clients = { IProjectsClient.class, IProjectConnectionClient.class, IResourcesClient.class, IRolesClient.class })
public class RemoteClientAutoConfiguration {
}
