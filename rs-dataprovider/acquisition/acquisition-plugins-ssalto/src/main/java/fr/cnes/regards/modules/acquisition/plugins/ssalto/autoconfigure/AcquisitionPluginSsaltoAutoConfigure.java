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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginsRepositoryProperties;

/**
 *
 * @author Christophe Mertz
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "regards.acquisition.ssalto", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ PluginsRepositoryProperties.class})
@EnableTransactionManagement
public class AcquisitionPluginSsaltoAutoConfigure {

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

}
