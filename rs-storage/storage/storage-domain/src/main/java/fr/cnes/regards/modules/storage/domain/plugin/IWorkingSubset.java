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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Represents a subset of {@link StorageDataFile} prepared by {@link INearlineDataStorage} plugins. <br>
 * Only the implementation of the plugin can dispatch storage action by bucket of {@link StorageDataFile} to handle.<br>
 * AIP service uses those subsets to run an asynchronous storage job for each one.
 * @author Sébastien Binda
 */
public interface IWorkingSubset {

    /**
     * Return the subset of {@link StorageDataFile}s to handle.
     * @return {@link Set}<{@link StorageDataFile}>
     */
    Set<StorageDataFile> getDataFiles();

}
