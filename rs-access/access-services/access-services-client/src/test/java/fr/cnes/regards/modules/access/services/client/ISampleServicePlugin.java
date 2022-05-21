/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IEntitiesServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.ISingleEntityServicePlugin;

/**
 * ISampleServicePlugin
 *
 * @author Christophe Mertz
 */
@PluginInterface(
    description = "Sample Catalog Service Plugin. Used to test any possible service return values (json, xml, image, octet-stream")
public interface ISampleServicePlugin extends IEntitiesServicePlugin, ISingleEntityServicePlugin {

}
