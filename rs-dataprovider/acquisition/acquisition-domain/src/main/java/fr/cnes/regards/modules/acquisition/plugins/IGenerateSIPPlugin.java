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
package fr.cnes.regards.modules.acquisition.plugins;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;

/**
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "Plugin to create product and file metadata")
public interface IGenerateSIPPlugin {

    // TODO CMZ : à confirmer que ça retourne une String
    public String createMetadataPlugin(List<AcquisitionFile> acqFiles, String datasetName)
            throws ModuleException;

    public String createMetaDataPlugin(List<AcquisitionFile> acqFiles);

}
