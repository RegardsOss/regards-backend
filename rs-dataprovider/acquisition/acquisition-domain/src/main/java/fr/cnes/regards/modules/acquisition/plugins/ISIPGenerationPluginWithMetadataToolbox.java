/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;
import java.util.SortedMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.utils.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;

/**
 * This plugin is used for generating product SIP on a legacy context (SIPAD-NG SSALTO feature)
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
@PluginInterface(
        description = "Plugin to generate SIP with product and file metadata using legacy metadata generation toolbox")
public interface ISIPGenerationPluginWithMetadataToolbox extends ISipGenerationPlugin {

    /**
     * Create metadata for acquisition file
     * @param acqFiles list of acquisition filoes
     * @return an ordered map of attribute
     * @throws ModuleException
     */
    SortedMap<Integer, Attribute> createMetadataPlugin(Set<AcquisitionFile> acqFiles) throws ModuleException;
}
