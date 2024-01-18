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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
public class FileRequestGroupDto {

    /**
     * Business request identifier
     */
    private String groupId;

    /**
     * Request type
     */
    private FileRequestType type;

    /**
     * Files in error status
     */
    private final Set<RequestResultInfoDto> errors = new HashSet<>();

    /**
     * Files in error status
     */
    private final Set<RequestResultInfoDto> success = new HashSet<>();

    /**
     * Request status
     */
    private FileGroupRequestStatus state;

    public FileRequestGroupDto(String groupId,
                               FileRequestType type,
                               FileGroupRequestStatus state,
                               Set<RequestResultInfoDto> errors,
                               Set<RequestResultInfoDto> success) {
        this.groupId = groupId;
        this.type = type;
        this.state = state;
        this.errors.addAll(errors);
        this.success.addAll(success);
    }

    public String getGroupId() {
        return groupId;
    }

    public FileRequestType getType() {
        return type;
    }

    public FileGroupRequestStatus getState() {
        return state;
    }

    public Set<RequestResultInfoDto> getErrors() {
        return errors;
    }

    public Set<RequestResultInfoDto> getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        String gid = (this.getGroupId() != null ? "groupId=" + this.getGroupId() + ", " : "");
        String s = (state != null ? "state=" + state + ", " : "");
        String t = (this.getType() != null ? "type=" + this.getType() + ", " : "");
        String err = (this.getErrors() != null ? "errors=" + this.getErrors() : "");
        return "FileRequestEvent [" + gid + s + t + err + "]";
    }
}
