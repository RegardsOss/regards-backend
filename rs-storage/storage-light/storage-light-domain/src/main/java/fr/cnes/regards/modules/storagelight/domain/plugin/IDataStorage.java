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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;

/**
 * Plugin interface for all storage systems.
 *
 * @author Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IDataStorage<T extends IWorkingSubset> {

    Collection<IWorkingSubset> prepare(Collection<FileReferenceRequest> fileReferenceRequest,
            StorageAccessModeEnum mode);

    void delete(T workingSet);

    void store(T workingSet, IProgressManager progressManager);

    InputStream retrieve(FileReference fileReference);

    void retrieve(T workingSet, Path destinationPath);
}
