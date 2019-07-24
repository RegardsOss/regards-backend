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

import java.nio.file.Path;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * Third <b>required</b> step of acquisition processing chain. This step is used to compute product name for a specified
 * file.
 *
 * @author Marc Sordi
 *
 */
@PluginInterface(description = "Product name computing plugin contract")
public interface IProductPlugin {

    /**
     * Compute product name for a specified file
     * @param filePath file path
     * @return product name
     * @throws ModuleException if error occurs!
     */
    String getProductName(Path filePath) throws ModuleException;
}
