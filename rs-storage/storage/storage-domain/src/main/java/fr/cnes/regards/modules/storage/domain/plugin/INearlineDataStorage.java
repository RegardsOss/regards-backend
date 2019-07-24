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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.nio.file.Path;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@PluginInterface(description = "Contract to respect by any NEARLINE data storage plugin")
public interface INearlineDataStorage<T extends IWorkingSubset> extends IDataStorage<T> {

    /**
     * Do the retrieve action for the given working subset.
     * @param workingSubset Subset of files to store.
     * @param destinationPath {@link Path} where to put retrieved files.
     * @param progressManager {@link IProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void retrieve(T workingSubset, Path destinationPath, IProgressManager progressManager);

}
