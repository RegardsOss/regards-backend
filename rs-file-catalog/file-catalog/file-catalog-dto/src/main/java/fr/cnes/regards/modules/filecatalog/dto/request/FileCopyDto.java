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
package fr.cnes.regards.modules.filecatalog.dto.request;

import org.springframework.util.Assert;

/**
 * Information about a file for a copy request.<br/>
 * Mandatory information are : <ul>
 * <li> Checksum of the file to delete</li>
 * <li> Storage location where to copy the file</li>
 * </ul>
 * See FilesCopyEvent for more information about deletion request process.
 *
 * @author Sébastien Binda
 */
public class FileCopyDto {

    /**
     * Checksum of the file to delete
     */
    private String checksum;

    /**
     * Storage location where to copy the file
     */
    private String storage;

    /**
     * Sub directory where to store file in the storage destination
     */
    private String subDirectory;

    /**
     * Source of the request
     */
    private String sessionOwner;

    /**
     * Session of the request
     */
    private String session;

    public String getChecksum() {
        return checksum;
    }

    public String getStorage() {
        return storage;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public static FileCopyDto build(String checksum, String storage, String sessionOwner, String session) {
        FileCopyDto request = new FileCopyDto();

        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");

        request.checksum = checksum;
        request.storage = storage;
        request.sessionOwner = sessionOwner;
        request.session = session;
        return request;
    }

    public static FileCopyDto build(String checksum,
                                    String storage,
                                    String subDirectory,
                                    String sessionOwner,
                                    String session) {
        FileCopyDto request = build(checksum, storage, sessionOwner, session);
        request.subDirectory = subDirectory;

        return request;
    }

    @Override
    public String toString() {
        return "FileCopyDto{"
               + "checksum='"
               + checksum
               + '\''
               + ", storage='"
               + storage
               + '\''
               + ", subDirectory='"
               + subDirectory
               + '\''
               + ", sessionOwner='"
               + sessionOwner
               + '\''
               + ", session='"
               + session
               + '\''
               + '}';
    }

}
