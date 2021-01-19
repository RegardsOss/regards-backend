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
package fr.cnes.regards.modules.storage.domain.dto.request;

import org.springframework.util.Assert;

import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;

/**
 * Information about a file for a deletion request.<br/>
 * Mandatory information are : <ul>
 *  <li> Checksum of the file to delete</li>
 *  <li> Storage location where to delete the file</li>
 *  <li> Owner of the file who ask for deletion </li>
 * </ul>
 * See {@link DeletionFlowItem} for more information about deletion request process.
 *
 * @author Sébastien Binda
 */
public class FileDeletionRequestDTO {

    /**
     * Checksum of the file to delete
     */
    private String checksum;

    /**
     * Storage location where to delete the file
     */
    private String storage;

    /**
     * Owner of the file who ask for deletion
     */
    private String owner;

    /**
     * Force file reference deletion when physical deletion on storage location is in error.<br/>
     * Can be useful if file doesn't exists anymore on storage location or if storage location is not accessible anymore.
     */
    private boolean forceDelete;

    public String getChecksum() {
        return checksum;
    }

    public String getStorage() {
        return storage;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isForceDelete() {
        return forceDelete;
    }

    /**
     * Build a new file deletion request information
     * @param checksum
     * @param storage
     * @param owner
     * @param forceDelete
     * @return {@link FileDeletionRequestDTO}
     */
    public static FileDeletionRequestDTO build(String checksum, String storage, String owner, boolean forceDelete) {
        FileDeletionRequestDTO request = new FileDeletionRequestDTO();

        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");

        request.checksum = checksum;
        request.owner = owner;
        request.storage = storage;
        request.forceDelete = forceDelete;
        return request;
    }

}
