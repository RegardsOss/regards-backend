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
package fr.cnes.regards.modules.storagelight.domain.plugin;

import java.util.Collection;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storagelight.domain.database.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;

/**
 * Plugin interface for all storage systems.
 *
 * @author Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IStorageLocation {

    Collection<FileReferenceWorkingSubset> prepareForStorage(Collection<FileReferenceRequest> fileReferenceRequests);

    Collection<FileDeletionWorkingSubset> prepareForDeletion(Collection<FileDeletionRequest> fileDeletionRequests);

    void delete(FileDeletionWorkingSubset workingSet, IDeletionProgressManager progressManager);

    void store(FileReferenceWorkingSubset workingSet, IStorageProgressManager progressManager);

    /**
     * Method called before each configuration update of this plugin to know if the modification is allowed or not.
     * The plugin implementation of this method should ensure that already stored files will always be accessible after
     * the modification.
     * @param newConfiguration {@link PluginConfiguration} with the new parameters for update
     * @param currentConfiguration {@link PluginConfiguration} with the current parameters before update.
     * @param filesAlreadyStored {@link boolean} Does files has been already stored with the current configuration ?
     * @return {@link PluginConfUpdatable} true if the plugin allows the modification. If not updatable contains the rejection cause
     */
    PluginConfUpdatable allowConfigurationUpdate(PluginConfiguration newConfiguration,
            PluginConfiguration currentConfiguration, boolean filesAlreadyStored);

}
