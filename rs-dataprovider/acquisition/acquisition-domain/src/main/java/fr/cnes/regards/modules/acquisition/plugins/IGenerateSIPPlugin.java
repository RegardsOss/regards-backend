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
import java.util.SortedMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;

/**
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "Plugin to create product and file metadata")
public interface IGenerateSIPPlugin {

    public SortedMap<Integer, Attribute> createMetadataPlugin(List<AcquisitionFile> acqFiles, String datasetName)
            throws ModuleException;

    public SortedMap<Integer, Attribute> createMetaDataPlugin(List<AcquisitionFile> acqFiles) throws ModuleException;
    
    public SIPCollection runPlugin(String sessionId, List<AcquisitionFile> acqFiles, String datasetName) throws ModuleException;
    
    public SIPCollection runPlugin(String sessionId, List<AcquisitionFile> acqFiles) throws ModuleException;

}
