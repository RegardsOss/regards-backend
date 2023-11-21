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
package fr.cnes.regards.modules.storage.domain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.exception.NearlineDownloadException;
import fr.cnes.regards.modules.storage.domain.exception.NearlineFileNotAvailableException;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import org.apache.commons.lang3.NotImplementedException;

import java.io.InputStream;

/**
 * Plugin to handle NEARLINE storage location. <br/>
 * A nearline storage location is a location where files cannot be accessed synchronously.<br/>
 * Files need to be restored in cache before they can be access for download.<br/>
 * See {@link AvailabilityFlowItem} for more information.
 *
 * @author Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any NEARLINE data storage plugin")
public interface INearlineStorageLocation extends IStorageLocation {

    /**
     * Do the retrieve action for the given working subset.
     *
     * @param workingSubset   Subset of files to restore.
     * @param progressManager {@link IRestorationProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void retrieve(FileRestorationWorkingSubset workingSubset, IRestorationProgressManager progressManager);

    /**
     * Download file if it is available in cache.
     *
     * @param fileReference the file's reference
     * @return input stream of file in cache
     * @throws NearlineFileNotAvailableException if the file is not available in the cache of NEARLINE storage
     * @throws NearlineDownloadException         if a error is raised during the downloading of file in the cache of NEARLINE storage
     */
    default InputStream download(FileReference fileReference)
        throws NearlineFileNotAvailableException, NearlineDownloadException {
        throw new NotImplementedException();
    }

}
