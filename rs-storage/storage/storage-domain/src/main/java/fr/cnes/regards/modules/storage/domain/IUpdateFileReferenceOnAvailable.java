/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

/**
 * Interface to implements as a spring bean to add customizable update action on a restored file.
 *
 * @author Sébastien Binda
 */
public interface IUpdateFileReferenceOnAvailable {

    /**
     * Update the {@link FileReference} restored.
     *
     * @param availableFileReference {@link FileReference} file reference restored
     * @param onlineFileLocation     {@link FileLocation} where the file available
     * @return {@link FileReference} updated file reference
     */
    FileReference update(FileReference availableFileReference, FileLocation onlineFileLocation) throws ModuleException;

}
