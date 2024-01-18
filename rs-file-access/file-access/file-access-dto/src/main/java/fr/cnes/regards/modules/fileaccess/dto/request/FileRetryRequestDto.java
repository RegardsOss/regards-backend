/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto.request;

import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Thibaud Michaudel
 **/
public class FileRetryRequestDto {

    /**
     * Request business identifier to retry
     */
    private String groupId;

    /**
     * Owners to retry errors requests
     */
    private final Collection<String> owners = new ArrayList<>();

    /**
     * Request type to retry
     */
    private FileRequestType type;

    public FileRetryRequestDto(String groupId, Collection<String> owners, FileRequestType type) {
        this.groupId = groupId;
        this.owners.addAll(owners);
        this.type = type;
    }

    public FileRetryRequestDto(String groupId, FileRequestType type) {
        this.groupId = groupId;
        this.type = type;
    }

    public FileRetryRequestDto(Collection<String> owners, FileRequestType type) {
        this.owners.addAll(owners);
        this.type = type;
    }

    public FileRetryRequestDto() {

    }

    public String getGroupId() {
        return groupId;
    }

    public FileRequestType getType() {
        return type;
    }

    public Collection<String> getOwners() {
        return owners;
    }

}
