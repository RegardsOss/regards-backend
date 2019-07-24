/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * This plugin is used for generating product SIP
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
@PluginInterface(description = "Plugin to generate SIP with product and file metadata")
public interface ISipGenerationPlugin {

    /**
     * Generate SIP according to specified {@link Product}
     * @param product {@link Product}
     * @return {@link SIP}
     * @throws ModuleException if error occurs!
     */
    SIP generate(Product product) throws ModuleException;
}
